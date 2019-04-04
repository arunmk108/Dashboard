package com.capitalone.dashboard.utils;

import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.model.quality.*;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CodeQualityMetricsConverter implements CodeQualityVisitor {

    private static final String TOTAL_NO_OF_TESTS = "tests";
    private static final String TEST_FAILURES = "test_failures";
    private static final String TEST_ERRORS = "test_errors";
    private static final String TEST_SUCCESS_DENSITY = "test_success_density";

    private static final String BLOCKER_VIOLATIONS = "blocker_violations";
    private static final String CRITICAL_VIOLATIONS = "critical_violations";
    private static final String MAJOR_VIOLCATIONS = "major_violations";
    private static final String VIOLATIONS = "violations";

    private static final String COVERAGE = "coverage";
    private static final String LINE_COVERAGE = "line_coverage";
    private static final String TOTAL_LINES_COVERED = "total_lines_covered";
    private static final String TOTAL_LINES_MISSED = "total_lines_missed";
    private static final String TOTAL_INSTRUCTIONS_COVERED = "total_instructions_covered";
    private static final String TOTAL_INSTRUCTIONS_MISSED = "total_instructions_missed";

    private final CodeQuality quality = new CodeQuality();

    // note for static analysis names are ,,,violations
    // function tests..

    @Override
    public void visit(JunitXmlReport report) {

        int testsPassed = report.getTests() - report.getFailures() - report.getErrors();

        Map<String, Pair<Integer, CodeQualityMetricStatus>> metricsMap = new HashMap<>();
        metricsMap.put(TOTAL_NO_OF_TESTS, Pair.of(report.getTests(), CodeQualityMetricStatus.Ok));
        metricsMap.put(TEST_FAILURES, Pair.of(report.getFailures(), report.getFailures() > 0 ? CodeQualityMetricStatus.Warning : CodeQualityMetricStatus.Ok));
        metricsMap.put(TEST_ERRORS, Pair.of(report.getErrors(), report.getErrors() > 0 ? CodeQualityMetricStatus.Alert : CodeQualityMetricStatus.Ok));
        metricsMap.put(TEST_SUCCESS_DENSITY, Pair.of(testsPassed, CodeQualityMetricStatus.Ok));

        if (null != report.getTimestamp()) {
            long timestamp = Math.max(quality.getTimestamp(), report.getTimestamp().toGregorianCalendar().getTimeInMillis());
            quality.setTimestamp(timestamp);
        }
        quality.setType(CodeQualityType.StaticAnalysis);

        // finally produce the result
        this.sumMetrics(metricsMap);

    }

    @Override
    public void visit(FindBugsXmlReport findBugReport) {
        Map<String, Pair<Integer, CodeQualityMetricStatus>> metricsMap = new HashMap<>();
        metricsMap.put(BLOCKER_VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(CRITICAL_VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(MAJOR_VIOLCATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));

        // loop over all the stuff in the report and accumulate violations.
        if (null != findBugReport.getFiles()) {
            findBugReport.getFiles().stream().forEach(bugFile -> {
                bugFile.getBugCollection().stream().forEach(
                        bugInstance -> {
                            switch (bugInstance.getPriority()) {
                                case Blocker: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(BLOCKER_VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Alert);
                                    metricsMap.put(BLOCKER_VIOLATIONS, newPair);
                                    break;
                                }
                                case Critical: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(CRITICAL_VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Alert);
                                    metricsMap.put(CRITICAL_VIOLATIONS, newPair);
                                    break;
                                }
                                case Normal: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(MAJOR_VIOLCATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Warning);
                                    metricsMap.put(MAJOR_VIOLCATIONS, newPair);
                                    break;
                                }
                                case Low: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Warning);
                                    metricsMap.put(VIOLATIONS, newPair);
                                    break;
                                }
                                default:
                                    // not recognised. ignore
                                    break;
                            }
                        }
                );
            });
        }


        // finally produce the result
        this.sumMetrics(metricsMap);
    }

    public void sumMetrics(Map<String,Pair<Integer, CodeQualityMetricStatus>> metricsMap) {
        Set<CodeQualityMetric> existingMetrics = quality.getMetrics();
        final Map<String, CodeQualityMetric> mapOfExistingMetrics = existingMetrics.stream().collect(Collectors.toMap(CodeQualityMetric::getName, Function.identity()));

        metricsMap.forEach((key, value) -> {
            CodeQualityMetric currentValue = mapOfExistingMetrics.get(key);
            CodeQualityMetric newValue = null;
            if (null == currentValue) {
                CodeQualityMetric codeQualityMetric = new CodeQualityMetric();
                codeQualityMetric.setName(key);
                codeQualityMetric.setFormattedValue(String.valueOf(value.getLeft()));
                codeQualityMetric.setValue(Integer.toString(value.getLeft()));
                codeQualityMetric.setStatus(value.getRight());
                newValue = codeQualityMetric;
            } else {
                // do the sum
                quality.getMetrics().remove(currentValue);
                newValue = new CodeQualityMetric(key);
                newValue.setValue(Integer.toString(Integer.parseInt(currentValue.getValue()) + value.getLeft()));
                newValue.setFormattedValue(String.valueOf(Integer.parseInt(currentValue.getValue()) + value.getLeft()));
                int newOrdinal = Math.max(value.getRight().ordinal(), currentValue.getStatus().ordinal());
                newValue.setStatus(CodeQualityMetricStatus.values()[newOrdinal]);
            }
            quality.addMetric(newValue);
        });
    }

    @Override
    public void visit(JacocoXmlReport jacocoXmlReport) {
        Map<String, Pair<Integer, CodeQualityMetricStatus>> metricsMap = new HashMap<>();
        metricsMap.put(TOTAL_LINES_COVERED, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(TOTAL_LINES_MISSED, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(TOTAL_INSTRUCTIONS_COVERED, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(TOTAL_INSTRUCTIONS_MISSED, Pair.of(0, CodeQualityMetricStatus.Ok));

        for (JacocoXmlReport.Counter counter:jacocoXmlReport.getCounters()) {
            switch (counter.getType()) {
                case LINE:
                    metricsMap.put(TOTAL_LINES_COVERED,Pair.of(counter.getCovered(),CodeQualityMetricStatus.Ok));
                    metricsMap.put(TOTAL_LINES_MISSED,Pair.of(counter.getMissed(),CodeQualityMetricStatus.Ok));
                    break;
                case INSTRUCTION:
                    metricsMap.put(TOTAL_INSTRUCTIONS_COVERED,Pair.of(counter.getCovered(),CodeQualityMetricStatus.Ok));
                    metricsMap.put(TOTAL_INSTRUCTIONS_MISSED, Pair.of(counter.getMissed(),CodeQualityMetricStatus.Ok));
                    break;
                default:
                    // no impl
                    break;
            }
        }
        this.sumMetrics(metricsMap);
        // now add in the missing one
        Map<String, CodeQualityMetric> codeQualityMetricMap = quality.getMetrics()
                .stream().collect(Collectors.toMap(CodeQualityMetric::getName, Function.identity()));
        final CodeQualityMetric lineCoverage = codeQualityMetricMap.remove(LINE_COVERAGE);
        quality.getMetrics().remove(lineCoverage);
        quality.addMetric(
                computeCoveragePercent(LINE_COVERAGE,
                        codeQualityMetricMap.get(TOTAL_LINES_COVERED),
                        codeQualityMetricMap.get(TOTAL_LINES_MISSED)));
        final CodeQualityMetric coverage = codeQualityMetricMap.remove(COVERAGE);
        quality.getMetrics().remove(coverage);
        quality.addMetric(
                computeCoveragePercent(COVERAGE,
                        codeQualityMetricMap.get(TOTAL_INSTRUCTIONS_COVERED),
                        codeQualityMetricMap.get(TOTAL_INSTRUCTIONS_MISSED)));


    }

    private CodeQualityMetric computeCoveragePercent(String metricName, CodeQualityMetric covered,CodeQualityMetric missed) {
        double percentageCovered = Double.parseDouble(covered.getValue())*100.0/(Double.parseDouble(covered.getValue()) + Double.parseDouble(missed.getValue()));
        CodeQualityMetric metric = new CodeQualityMetric(metricName);
        metric.setFormattedValue(String.format(Locale.US,"%.3f",percentageCovered));
        metric.setValue(Double.toString(percentageCovered));
        metric.setStatus(CodeQualityMetricStatus.Ok);
        return metric;
    }


    @Override
    public void visit(PmdReport pmdReport) {
        Map<String, Pair<Integer, CodeQualityMetricStatus>> metricsMap = new HashMap<>();
        metricsMap.put(BLOCKER_VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(CRITICAL_VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(MAJOR_VIOLCATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));

        // loop over all the stuff in the report and accumulate violations.
        if (null != pmdReport.getFiles()) {
            pmdReport.getFiles().stream().forEach(violationFile -> {
                violationFile.getViolations().stream().forEach(
                        violation -> {
                            switch (violation.getPriority()) {
                                case 1: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(BLOCKER_VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Alert);
                                    metricsMap.put(BLOCKER_VIOLATIONS, newPair);
                                    break;
                                }
                                case 2: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(CRITICAL_VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Alert);
                                    metricsMap.put(CRITICAL_VIOLATIONS, newPair);
                                    break;
                                }
                                case 3: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(MAJOR_VIOLCATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Warning);
                                    metricsMap.put(MAJOR_VIOLCATIONS, newPair);
                                    break;
                                }
                                default:
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Warning);
                                    metricsMap.put(VIOLATIONS, newPair);
                                    break;
                            }
                        }
                );
            });
        }

        // finally produce the result
        this.sumMetrics(metricsMap);
    }

    @Override
    public void visit(CheckstyleReport checkstyleReport) {
        Map<String, Pair<Integer, CodeQualityMetricStatus>> metricsMap = new HashMap<>();
        metricsMap.put(BLOCKER_VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(CRITICAL_VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(MAJOR_VIOLCATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));
        metricsMap.put(VIOLATIONS, Pair.of(0, CodeQualityMetricStatus.Ok));

        // loop over all the stuff in the report and accumulate violations.
        if (null != checkstyleReport.getFiles()) {
            checkstyleReport.getFiles().stream().forEach(violationFile -> {
                if (null != violationFile.getErrors()) {
                    violationFile.getErrors().stream().forEach(
                        violation -> {
                            switch (violation.getSeverity()) {
                                case error: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(BLOCKER_VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Alert);
                                    metricsMap.put(BLOCKER_VIOLATIONS, newPair);
                                    break;
                                }
                                case warning: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(CRITICAL_VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Alert);
                                    metricsMap.put(CRITICAL_VIOLATIONS, newPair);
                                    break;
                                }
                                case info: {
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(MAJOR_VIOLCATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Warning);
                                    metricsMap.put(MAJOR_VIOLCATIONS, newPair);
                                    break;
                                }
                                default:
                                    final Pair<Integer, CodeQualityMetricStatus> metricStatusPair = metricsMap.get(VIOLATIONS);
                                    final Pair newPair = Pair.of(metricStatusPair.getLeft().intValue() + 1, CodeQualityMetricStatus.Warning);
                                    metricsMap.put(VIOLATIONS, newPair);
                                    break;
                            }
                        }
                    );
                }
            });
        }

        // finally produce the result
        this.sumMetrics(metricsMap);
    }

    @Override
    public CodeQuality produceResult() {
        return quality;
    }

}
