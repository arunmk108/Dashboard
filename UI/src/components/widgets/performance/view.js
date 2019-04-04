(function () {
    'use strict';

    angular
        .module(HygieiaConfig.module)
        .controller('performanceViewController', performanceViewController);

    performanceViewController.$inject = ['$q', '$scope','performanceData', '$uibModal', 'collectorData'];
    function performanceViewController($q, $scope, performanceData, $uibModal, collectorData) {
        var ctrl = this;

        ctrl.callsChartOptions = {
            plugins: [
                Chartist.plugins.gridBoundaries(),
                Chartist.plugins.lineAboveArea(),
                Chartist.plugins.pointHalo(),
                Chartist.plugins.ctPointClick({

                }),
                Chartist.plugins.ctAxisTitle({
                    axisX: {
                        axisTitle: 'Timestamp',
                        axisClass: 'ct-axis-title',
                        offset: {
                            x: 0,
                            y: 50
                        },
                        textAnchor: 'middle'
                    }
                }),
                Chartist.plugins.ctPointLabels({
                    textAnchor: 'middle'
                })
            ],
            //low: 0,
            chartPadding: {
                right: 35,
                top: 20
            },
            showArea: true,
            lineSmooth: false,
            fullWidth: true,
            axisY: {
                allowDecimals: false,
                offset: 30,
                showGrid: true,
                showLabel: true,
                labelInterpolationFnc: function(value) {return Math.round(value * 100)/100;}
            }
        };

        ctrl.errorsChartOptions = {
            plugins: [
                Chartist.plugins.gridBoundaries(),
                Chartist.plugins.lineAboveArea(),
                Chartist.plugins.pointHalo(),
                Chartist.plugins.ctPointClick({

                }),
                Chartist.plugins.ctAxisTitle({
                    axisX: {
                        axisTitle: 'Timestamp',
                        axisClass: 'ct-axis-title',
                        offset: {
                            x: 0,
                            y: 50
                        },
                        textAnchor: 'middle'
                    }
                }),
                Chartist.plugins.ctPointLabels({
                    textAnchor: 'middle'
                })
            ],
            //low: 0,
            chartPadding: {
                right: 35,
                top: 20
            },
            showArea: true,
            lineSmooth: false,
            fullWidth: true,
            axisY: {
                allowDecimals: false,
                offset: 30,
                showGrid: true,
                showLabel: true,
                labelInterpolationFnc: function(value) {return Math.round(value * 100)/100;}
            }
        };

        ctrl.pieOptions = {
            donut: true,
            donutWidth: 20,
            startAngle: 270,
            total: 200,
            showLabel: false
        };

        ctrl.load = function() {

            var deferred = $q.defer();
            var params = {
                componentId: $scope.widgetConfig.componentId,
            };

            console.log($scope.widgetConfig.componentId);
            var count =0;
            collectorData.itemsByType('appPerformance').then(function(data){
                data.forEach(function(element){
                    if (element.enabled){
                        ctrl.appname = element.description;
                        ctrl.appID = element.options.appID;
                        ctrl.appname2 = element.options.appName;
                        count++;
                    }

                });



                performanceData.appPerformance({componentId: $scope.widgetConfig.componentId,max:20}).then(function(data) {
                    processResponse(data.result);
                    deferred.resolve(data.lastUpdated);
                });
            });
            return deferred.promise;
        };

        ctrl.showDetail = showDetail;

        function showDetail(evt){

            $uibModal.open({
                controller: 'PerformanceDetailController',
                controllerAs: 'detail',
                templateUrl: 'components/widgets/performance/detail.html',
                size: 'lg',
                resolve: {
                    index: function(){
                        return evt;
                    },
                    warnings: function(){
                        return ctrl.warning;
                    },
                    good: function(){
                        return ctrl.good;
                    },
                    bad: function(){
                        return ctrl.bad;
                    }
                }
            });
        }

        function processResponse(data) {
            var groupedCallsData = [];
            var groupedErrorsData = [];
            var calllabels = [];
            var errorlabels = [];
            var errorcount = 0;
            var callcount = 0;
            var responsecount = 0;
            var nodehealth = 0;
            var businesshealth = 0;
            var errorspm = 0;
            var callspm = 0;
            var responsetime = 0;
            var healthruleviolations = [];
            var warnings = [];
            var good = [];
            var bad = [];


            var metrics = _(data).sortBy('timeStamp').__wrapped__[0].metrics;
            var collectorItemId = data[0];
            var cId = collectorItemId.collectorItemId;
            collectorData.getCollectorItemById(cId).then(function(result) {
                    var res = result;
                    ctrl.appname = res.description;
                }
            );

            for(var metric in metrics) {
                if (metric === 'businessTransactionHealthPercent'){
                    ctrl.businessavg = Math.round(metrics[metric]*100 *10)/10;
                }
                if (metric === 'nodeHealthPercent'){
                    ctrl.nodeavg = Math.round(metrics[metric]*100 *10)/10;
                }
                if (metric === 'errorRateSeverity'){
                    ctrl.errorvalue = metrics[metric];
                }
                if (metric === 'responseTimeSeverity'){
                    ctrl.responsevalue = metrics[metric];
                }
                if (metric === 'violationObject'){
                    ctrl.violations = metrics[metric];
                }
            }

            ctrl.violations.forEach(function(element){
                if (element.severity === "WARNING"){
                    if (element.incidentStatus === "OPEN") warnings.push(element);
                    else good.push(element);
                }else {
                    bad.push(element);
                }
            });

            ctrl.warning = warnings;
            ctrl.good = good;
            ctrl.bad = bad;

            _(data).sortBy('timeStamp').reverse().forEach(function(element){
                var metrictime = element.timestamp;
                var mins = (metrictime/60000) % 60;
                var hours = (((metrictime/60/60000) % 24) + 19) % 24;

                var metrics = element.metrics;

                for(var metric in metrics) {
                    if (metric === "violationObject"){
                        healthruleviolations.push({
                            metrictime: metrictime,
                            value: metrics[metric]});
                    }
                    if (metric === "errorsperMinute" && metrics[metric]>0){
                        errorcount++;
                        errorspm += metrics[metric];
                        groupedErrorsData.push(metrics[metric]);
                        errorlabels.push(Math.floor(hours) + ":" + Math.round(mins));
                    }
                    if (metric === 'errorRateSeverity'){
                        ctrl.errorvalue = metrics[metric];
                    }
                    if (metric === "callsperMinute" && metrics[metric]>0){
                        callcount++;
                        callspm += metrics[metric];
                        groupedCallsData.push(metrics[metric]);
                        calllabels.push(Math.floor(hours) + ":" + Math.round(mins));
                    }
                    if (metric === "averageResponseTime" && metrics[metric]>0){
                        responsecount++;
                        responsetime += metrics[metric];
                    }
                }
            });
            ctrl.healthruleviolations = healthruleviolations.slice(healthruleviolations.length-7, healthruleviolations.length);
            ctrl.groupedCallsData = groupedCallsData;
            ctrl.groupedErrorsData = groupedErrorsData;
            ctrl.errorlabels = errorlabels;
            ctrl.calllabels = calllabels;

            if (errorcount!=0) errorspm = Math.round(errorspm/errorcount * 10)/10;
            else errorspm = 'No Data Collected';
            if (responsecount!=0) responsetime = Math.round(responsetime/responsecount * 10)/10;
            else responsetime = 'No Data Collected';
            if (callcount!=0) callspm = Math.round(callspm/callcount * 10)/10;
            else callspm = 'No Data Collected';

            ctrl.errorspm = errorspm;
            ctrl.callspm = callspm;
            ctrl.responsetime = responsetime;


            ctrl.transactionHealthData = {
                series: [ctrl.businessavg, 100-ctrl.businessavg]
            };

            ctrl.nodeHealthData = {
                series: [ctrl.nodeavg, 100-ctrl.nodeavg]
            };

            ctrl.callsChartData = {
                series: [groupedCallsData.slice(groupedCallsData.length-7, groupedCallsData.length)],
                labels: calllabels.slice(calllabels.length-7, calllabels.length)
            };

            ctrl.errorsChartData = {
                series: [groupedErrorsData.slice(groupedErrorsData.length-7, groupedErrorsData.length)],
                labels: errorlabels.slice(errorlabels.length-7, errorlabels.length)
            };
        }

    }
})();
