package com.capitalone.dashboard.collector;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.capitalone.dashboard.model.CloudInstance;
import com.capitalone.dashboard.model.CloudVolumeStorage;
import com.capitalone.dashboard.model.NameValue;
import com.capitalone.dashboard.repository.CloudInstanceRepository;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest({DefaultAWSCloudClient.class})
public class DefaultAWSCloudClientTest {

    private AWSCloudSettings settings = mock(AWSCloudSettings.class);

    private AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);

    private AmazonCloudWatchClient cloudWatchClient = mock(AmazonCloudWatchClient.class);

    private AmazonAutoScalingClient autoScalingClient = mock(AmazonAutoScalingClient.class);

    private CloudInstanceRepository cloudInstanceRepository = mock(CloudInstanceRepository.class);

    private static DefaultAWSCloudClient defaultAWSCloudClient;

    private static DescribeInstancesResult describeInstancesResult;
    private static DescribeAutoScalingInstancesResult describeAutoScalingInstancesResult;
    private static DescribeVolumesResult describeVolumesResult;
    private static GetMetricStatisticsResult cpuMetric;
    private static GetMetricStatisticsResult networkInMetric;
    private static GetMetricStatisticsResult networkOutMetric;
    private static GetMetricStatisticsResult diskInMetric;
    private static GetMetricStatisticsResult diskOutMetric;
    private static GetMetricStatisticsResult estimatedCharge;
    private static String ACCOUNT = "123456789123";


    @BeforeClass
    public static void setupTestObjects() throws IOException {

        Gson gson = new Gson();
        byte[] content = Resources.asByteSource(Resources.getResource("describeInstance.json")).read();
        describeInstancesResult = gson.fromJson(new String(content), DescribeInstancesResult.class);
        content = Resources.asByteSource(Resources.getResource("autoScaleResult.json")).read();
        describeAutoScalingInstancesResult = gson.fromJson(new String(content), DescribeAutoScalingInstancesResult.class);
        content = Resources.asByteSource(Resources.getResource("describeVolumesResult.json")).read();
        describeVolumesResult = gson.fromJson(new String(content), DescribeVolumesResult.class);
        content = Resources.asByteSource(Resources.getResource("cpuUsagemetric.json")).read();
        cpuMetric = gson.fromJson(new String(content), GetMetricStatisticsResult.class);

        content = Resources.asByteSource(Resources.getResource("diskRead.json")).read();
        diskInMetric = gson.fromJson(new String(content), GetMetricStatisticsResult.class);

        content = Resources.asByteSource(Resources.getResource("diskWrite.json")).read();
        diskOutMetric = gson.fromJson(new String(content), GetMetricStatisticsResult.class);

        content = Resources.asByteSource(Resources.getResource("networkIn.json")).read();
        networkInMetric = gson.fromJson(new String(content), GetMetricStatisticsResult.class);

        content = Resources.asByteSource(Resources.getResource("networkOut.json")).read();
        networkOutMetric = gson.fromJson(new String(content), GetMetricStatisticsResult.class);

        content = Resources.asByteSource(Resources.getResource("estimatedCharge.json")).read();
        estimatedCharge = gson.fromJson(new String(content),GetMetricStatisticsResult.class);

    }

    @Before
    public void setupTest() throws Exception {
        // prevent pollution between tests by having a new one every time
        settings = mock(AWSCloudSettings.class);

        mock(AmazonEC2Client.class);

        mock(AmazonCloudWatchClient.class);

        mock(AmazonAutoScalingClient.class);

        mock(CloudInstanceRepository.class);

        PowerMockito.whenNew(AmazonEC2Client.class).withAnyArguments().thenReturn(ec2Client);
        PowerMockito.whenNew(AmazonAutoScalingClient.class).withAnyArguments().thenReturn(autoScalingClient);
        PowerMockito.whenNew(AmazonCloudWatchClient.class).withAnyArguments().thenReturn(cloudWatchClient);

        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
    }

    @Test
    public void getCloudInstances() throws Exception {
        when(ec2Client.describeInstances()).thenReturn(describeInstancesResult);
        when(autoScalingClient.describeAutoScalingInstances()).thenReturn(describeAutoScalingInstancesResult);


        defaultAWSCloudClient.setEc2Client(ec2Client);
        defaultAWSCloudClient.setAutoScalingClient(autoScalingClient);
        defaultAWSCloudClient.setCloudWatchClient(cloudWatchClient);
        when(cloudWatchClient.getMetricStatistics(any(GetMetricStatisticsRequest.class))).thenReturn(new GetMetricStatisticsResult());
        Map<String, List<CloudInstance>> results = defaultAWSCloudClient.getCloudInstances(cloudInstanceRepository);
        assertEquals(results.size(),1);
        assertEquals(results.keySet().size(), 1);
        Collection<CloudInstance> instances = results.get("123456789123");
        assertEquals(instances.size(), 2);
        CloudInstance instance1 = (CloudInstance) instances.toArray()[0];
        CloudInstance instance2 = (CloudInstance) instances.toArray()[1];
        assertEquals(instance1.getAccountNumber(), "123456789123");
        assertEquals(instance2.getAccountNumber(), "123456789123");
        assertEquals(instance1.getInstanceId(), "i-12345678");
        assertEquals(instance2.getInstanceId(), "i-23456789");
        assertEquals(instance1.getAutoScaleName(), "myauto-KFK-uat-develop-aaa-1");
        assertEquals(instance2.getAutoScaleName(), "NONE");
        assertEquals(instance1.getImageId(), "ami-12345678");
        assertEquals(instance2.getImageId(), "ami-23456789");
        assertEquals(instance1.getInstanceType(), "c3.8xlarge");
        assertEquals(instance2.getInstanceType(), "m3.large");
        assertEquals(instance1.getSubnetId(), "subnet-12345678");
        assertEquals(instance2.getSubnetId(), "subnet-12345678");
        assertEquals(instance1.getVirtualNetworkId(), "vpc-12345678");
        assertEquals(instance2.getVirtualNetworkId(), "vpc-12345678");
        assertEquals(instance1.getTags().size(), 2);
        assertEquals(instance2.getTags().size(), 1);
        NameValue tag1_1 = instance1.getTags().get(0);
        NameValue tag1_2 = instance1.getTags().get(1);
        NameValue tag2_1 = instance2.getTags().get(0);
        assertEquals(tag1_1.getName(), "Name");
        assertEquals(tag1_1.getValue(), "Value");
        assertEquals(tag1_2.getName(), "Env");
        assertEquals(tag1_2.getValue(), "MyAwesomeEnv");
        assertEquals(tag2_1.getName(), "Env");
        assertEquals(tag2_1.getValue(), "Env2");
        verify(cloudWatchClient, times(10)).getMetricStatistics(any(GetMetricStatisticsRequest.class));
    }


    @Test
    public void getCloudInstancesEmpty() throws Exception {
        when(ec2Client.describeInstances()).thenReturn(new DescribeInstancesResult());
        when(autoScalingClient.describeAutoScalingInstances()).thenReturn(new DescribeAutoScalingInstancesResult());

        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setEc2Client(ec2Client);
        defaultAWSCloudClient.setAutoScalingClient(autoScalingClient);
        defaultAWSCloudClient.setCloudWatchClient(cloudWatchClient);
        when(cloudWatchClient.getMetricStatistics(any(GetMetricStatisticsRequest.class))).thenReturn(new GetMetricStatisticsResult());
        Map<String, List<CloudInstance>> results = defaultAWSCloudClient.getCloudInstances(cloudInstanceRepository);
        assertEquals(results.size(), 0);
        verify(cloudWatchClient, times(0)).getMetricStatistics(any(GetMetricStatisticsRequest.class));
    }

    @Test
    public void getCloudVolumes() throws Exception {
        when(ec2Client.describeVolumes()).thenReturn(describeVolumesResult);
        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setEc2Client(ec2Client);

        Map<String, String> iaMap = new HashMap<>();
        iaMap.put("i-12345678", ACCOUNT);
        iaMap.put("i-23456789", ACCOUNT);

        Map<String, List<CloudVolumeStorage>> results = defaultAWSCloudClient.getCloudVolumes(iaMap);

        assertEquals(results.size(), 2);
        Collection<CloudVolumeStorage> accVolume = results.get(ACCOUNT);
        Collection<CloudVolumeStorage> noAccVol = results.get("NOACCOUNT");
        assertEquals(accVolume.size(), 2);
        assertEquals(noAccVol.size(), 1);
        CloudVolumeStorage aVol1 = (CloudVolumeStorage) accVolume.toArray()[0];
        CloudVolumeStorage aVol2 = (CloudVolumeStorage) accVolume.toArray()[1];
        CloudVolumeStorage naVol1 = (CloudVolumeStorage) noAccVol.toArray()[0];
        assertEquals(aVol1.getAccountNumber(), ACCOUNT);
        assertEquals(aVol2.getAccountNumber(), ACCOUNT);
        assertEquals(naVol1.getAccountNumber(), "NOACCOUNT");

        assertEquals(aVol1.getVolumeId(), "vol-12345678");
        assertEquals(aVol2.getVolumeId(), "vol-98765432");
        assertEquals(naVol1.getVolumeId(), "vol-87654321");

        assertEquals(aVol1.isEncrypted(), false);
        assertEquals(aVol2.isEncrypted(), false);
        assertEquals(naVol1.isEncrypted(), true);

        assertEquals(aVol1.getAttachInstances().size(), 1);
        assertEquals(aVol2.getAttachInstances().size(), 1);
        assertEquals(aVol1.getAttachInstances().get(0), "i-12345678");
        assertEquals(aVol2.getAttachInstances().get(0), "i-23456789");
        assertEquals(naVol1.getAttachInstances().size(), 0);
    }



    @Test
    public void getCloudVolumesEmpty() throws Exception {
        when(ec2Client.describeVolumes()).thenReturn(new DescribeVolumesResult());
        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setEc2Client(ec2Client);

        Map<String, String> iaMap = new HashMap<>();
        iaMap.put("i-12345678", ACCOUNT);
        iaMap.put("i-23456789", ACCOUNT);

        Map<String, List<CloudVolumeStorage>> results = defaultAWSCloudClient.getCloudVolumes(iaMap);

        assertEquals(results.size(), 0);
    }

    @Test
    public void get24HourInstanceEstimatedCharge() throws Exception {
        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setCloudWatchClient(cloudWatchClient);
        when(cloudWatchClient.getMetricStatistics(any())).thenReturn(estimatedCharge);
        assertEquals(defaultAWSCloudClient.get24HourInstanceEstimatedCharge(), new Double(9999.9999));
    }

    @Test
    public void getInstanceCPUSinceLastRun() throws Exception {
        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setCloudWatchClient(cloudWatchClient);
        when(cloudWatchClient.getMetricStatistics(any())).thenReturn(cpuMetric);
        assertEquals(defaultAWSCloudClient.getInstanceCPUSinceLastRun("i-12345678",new Date().getTime()), new Double(1.263));
    }

    @Test
    public void getLastHourInstanceNetworkIn() throws Exception {
        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setCloudWatchClient(cloudWatchClient);
        when(cloudWatchClient.getMetricStatistics(any())).thenReturn(networkInMetric);
        assertEquals(defaultAWSCloudClient.getLastHourInstanceNetworkIn("i-12345678",new Date().getTime()), new Double(577673.7166666667));
    }

    @Test
    public void getLastHourIntanceNetworkOut() throws Exception {
        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setCloudWatchClient(cloudWatchClient);
        when(cloudWatchClient.getMetricStatistics(any())).thenReturn(networkOutMetric);
        assertEquals(defaultAWSCloudClient.getLastHourIntanceNetworkOut("i-12345678",new Date().getTime()), new Double(178259.4));
    }

    @Test
    public void getLastHourInstanceDiskRead() throws Exception {
        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setCloudWatchClient(cloudWatchClient);
        when(cloudWatchClient.getMetricStatistics(any())).thenReturn(diskInMetric);
        assertEquals(defaultAWSCloudClient.getLastHourInstanceDiskRead("i-12345678",new Date().getTime()), new Double(0));
    }

    @Test
    public void getLastInstanceHourDiskWrite() throws Exception {
        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setCloudWatchClient(cloudWatchClient);
        when(cloudWatchClient.getMetricStatistics(any())).thenReturn(diskOutMetric);
        assertEquals(defaultAWSCloudClient.getLastInstanceHourDiskWrite("i-12345678"), new Double(0));
    }

    @Test
    public void canUseFiltersIfDefined() throws Exception {

        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        Map<String,List<String>> filterProperties = new HashMap<String,List<String>>();
        filterProperties.put("tag: myTag", Arrays.asList("tagValue"));
        filterProperties.put("instance-state-name", Arrays.asList("running","pending"));
        when(settings.getFilters()).thenReturn(filterProperties);

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setEc2Client(ec2Client);

        when(autoScalingClient.describeAutoScalingInstances()).thenReturn(new DescribeAutoScalingInstancesResult());
        defaultAWSCloudClient.setAutoScalingClient(autoScalingClient);

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(new DescribeInstancesResult());

        //do the test
        defaultAWSCloudClient.getCloudInstances(cloudInstanceRepository);

        // verify
        ArgumentCaptor<DescribeInstancesRequest> captor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);
        verify(ec2Client).describeInstances(captor.capture());

        assertThat(captor.getValue().getFilters(), hasSize(2));
        assertThat(captor.getValue().getFilters(), containsInAnyOrder(
                new Filter("tag: myTag").withValues("tagValue"),
                new Filter("instance-state-name").withValues("running","pending")));

    }

    @Test
    public void ignoresFiltersIfNull() throws Exception {

        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");

        when(settings.getFilters()).thenReturn(null);

        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);
        defaultAWSCloudClient.setEc2Client(ec2Client);

        when(autoScalingClient.describeAutoScalingInstances()).thenReturn(new DescribeAutoScalingInstancesResult());
        defaultAWSCloudClient.setAutoScalingClient(autoScalingClient);

        when(ec2Client.describeInstances()).thenReturn(new DescribeInstancesResult());

        //do the test
        defaultAWSCloudClient.getCloudInstances(cloudInstanceRepository);

        // verify.. should not throw exception;

    }

    @Test
    public void regionCanBeSet() throws Exception {
        PowerMockito.whenNew(AmazonEC2Client.class).withAnyArguments().thenReturn(ec2Client);
        PowerMockito.whenNew(AmazonAutoScalingClient.class).withAnyArguments().thenReturn(autoScalingClient);
        PowerMockito.whenNew(AmazonCloudWatchClient.class).withAnyArguments().thenReturn(cloudWatchClient);

        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");
        when(settings.getRegion()).thenReturn(Regions.EU_WEST_1);

        //test
        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);

        verify(ec2Client).withRegion(same(Regions.EU_WEST_1));
        verify(cloudWatchClient).withRegion(same(Regions.EU_WEST_1));
        verify(autoScalingClient).withRegion(same(Regions.EU_WEST_1));
    }

    @Test
    public void nullRegionIgnored() throws Exception {
        PowerMockito.whenNew(AmazonEC2Client.class).withAnyArguments().thenReturn(ec2Client);
        PowerMockito.whenNew(AmazonAutoScalingClient.class).withAnyArguments().thenReturn(autoScalingClient);
        PowerMockito.whenNew(AmazonCloudWatchClient.class).withAnyArguments().thenReturn(cloudWatchClient);


        when(settings.getProxyHost()).thenReturn("http://myproxy.com");
        when(settings.getProxyPort()).thenReturn("8080");
        when(settings.getProfile()).thenReturn("ABCDEG");
        when(settings.getNonProxy()).thenReturn("localhost");
        when(settings.getRegion()).thenReturn(null);



        //test
        defaultAWSCloudClient = new DefaultAWSCloudClient(settings);

        verify(ec2Client, times(0)).withRegion(same(Regions.EU_WEST_1));
        verify(autoScalingClient, times(0)).withRegion(same(Regions.EU_WEST_1));
        verify(cloudWatchClient, times(0)).withRegion(same(Regions.EU_WEST_1));
    }

}