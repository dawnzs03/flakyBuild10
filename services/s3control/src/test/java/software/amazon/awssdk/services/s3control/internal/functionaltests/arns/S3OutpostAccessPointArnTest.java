/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.awssdk.services.s3control.internal.functionaltests.arns;


import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.S3ControlClientBuilder;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class S3OutpostAccessPointArnTest extends S3ControlWireMockTestBase {
    private S3ControlClient s3;
    private static final String URL_PREFIX = "/v20180820/accesspoint/";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void methodSetUp() {
        s3 = buildClient();
    }

    @Test
    public void dualstackEnabled_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().serviceConfiguration(b -> b.dualstackEnabled(true)).build();

        String outpostArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Outpost Access Points do not support dual-stack");
        s3ControlForTest.getAccessPoint(b -> b.name(outpostArn));
    }

    @Test
    public void dualstackEnabledViaClient_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().dualstackEnabled(true).build();

        String outpostArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Outpost Access Points do not support dual-stack");
        s3ControlForTest.getAccessPoint(b -> b.name(outpostArn));
    }

    @Test
    public void malformedArn_MissingOutpostSegment_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String outpostArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid ARN: The Outpost Id was not set");
        s3ControlForTest.getAccessPoint(b -> b.name(outpostArn));
    }

    @Test
    public void malformedArn_MissingAccessPointSegment_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String outpostArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid ARN: Expected a 4-component resource");
        s3ControlForTest.getAccessPoint(b -> b.name(outpostArn));
    }

    @Test
    public void malformedArn_MissingAccessPointName_shouldThrowException() {
        S3ControlClient s3ControlForTest = buildClientCustom().build();

        String outpostArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:myaccesspoint";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Expected a 4-component resource");
        s3ControlForTest.getAccessPoint(b -> b.name(outpostArn));
    }

    @Test
    public void bucketArnDifferentRegionNoConfigFlag_throwsIllegalArgumentException() {
        S3ControlClient s3ControlForTest = initializedBuilder().region(Region.of("us-west-2")).build();
        String outpostArn = "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";

        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid configuration: region from ARN `us-east-1` does not match client region `us-west-2` and UseArnRegion is `false`");
        s3ControlForTest.getAccessPoint(b -> b.name(outpostArn));
    }

    @Test
    public void outpostArn_accountIdPresent_shouldThrowException() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-future-1")).build();

        String outpostArn = "arn:aws:s3-outposts:us-future-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";
        exception.expect(SdkClientException.class);
        exception.expectMessage("Invalid ARN: the accountId specified in the ARN (`123456789012`) does not match the parameter (`1234`)");

        s3Control.getAccessPoint(b -> b.name(outpostArn).accountId("1234"));
    }

    @Test
    public void nonArn_shouldNotRedirect() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-west-2")).build();
        String name = "myaccesspoint";

        String expectedUrl = getUrl(name);

        stubResponse(expectedUrl);
        s3Control.getAccessPoint(b -> b.name(name).accountId("123456789012"));
        String expectedHost = "123456789012.s3-control.us-west-2.amazonaws.com";

        verify(getRequestedFor(urlEqualTo(expectedUrl))
                   .withHeader("authorization", containing("us-west-2/s3/aws4_request"))
                   .withHeader("x-amz-account-id", equalTo("123456789012")));
        assertThat(getRecordedEndpoints().size(), is(1));
        assertThat(getRecordedEndpoints().get(0).getHost(), is(expectedHost));
    }

    @Test
    public void outpostArnUSRegion() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-west-2")).build();

        String outpostArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";
        String expectedHost = "s3-outposts.us-west-2.amazonaws.com";

        String expectedUrl = getUrl(outpostArn);

        stubResponse(expectedUrl);

        s3Control.getAccessPoint(b -> b.name(outpostArn).accountId("123456789012"));

        verifyOutpostRequest("us-west-2", expectedUrl, expectedHost);
    }

    @Test
    public void outpostArn_GovRegion() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-gov-east-1")).build();

        String outpostArn = "arn:aws-us-gov:s3-outposts:us-gov-east-1:123456789012:outpost:op-01234567890123456:accesspoint"
                            + ":myaccesspoint";
        String expectedHost = "s3-outposts.us-gov-east-1.amazonaws.com";

        String expectedUrl = getUrl(outpostArn);

        stubResponse(expectedUrl);

        s3Control.getAccessPoint(b -> b.name(outpostArn));

        verifyOutpostRequest("us-gov-east-1", expectedUrl, expectedHost);
    }

    @Test
    public void outpostArn_futureRegion_US() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("us-future-1")).build();


        String outpostArn = "arn:aws:s3-outposts:us-future-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";
        String expectedHost = "s3-outposts.us-future-1.amazonaws.com";

        String expectedUrl = getUrl(outpostArn);

        stubResponse(expectedUrl);

        s3Control.getAccessPoint(b -> b.name(outpostArn));

        verifyOutpostRequest("us-future-1", expectedUrl, expectedHost);
    }

    @Test
    public void outpostArn_futureRegion_CN() {
        S3ControlClient s3Control = initializedBuilderForAccessPoint().region(Region.of("cn-future-1")).build();
        String outpostArn = "arn:aws-cn:s3-outposts:cn-future-1:123456789012:outpost:op-01234567890123456:accesspoint"
                            + ":myaccesspoint";

        String expectedHost = "s3-outposts.cn-future-1.amazonaws.com.cn";

        String expectedUrl = getUrl(outpostArn);

        stubResponse(expectedUrl);

        s3Control.getAccessPoint(b -> b.name(outpostArn));
        verifyOutpostRequest("cn-future-1", expectedUrl, expectedHost);
    }

    @Test
    public void outpostArnDifferentRegion_useArnRegionSet_shouldUseRegionFromArn() {
        String outpostArn = "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";
        String expectedHost = "s3-outposts.us-east-1.amazonaws.com";

        String expectedUrl = getUrl(outpostArn);
        stubResponse(expectedUrl);
        S3ControlClient s3WithUseArnRegion =
            initializedBuilderForAccessPoint().region(Region.of("us-west-2")).serviceConfiguration(b -> b.useArnRegionEnabled(true)).build();
        s3WithUseArnRegion.getAccessPoint(b -> b.name(outpostArn));
        verifyOutpostRequest("us-east-1", expectedUrl, expectedHost);
    }

    private S3ControlClientBuilder initializedBuilderForAccessPoint() {
        return initializedBuilder();
    }

    private String getUrl(String arn) {
        return URL_PREFIX + SdkHttpUtils.urlEncode(arn);
    }
}
