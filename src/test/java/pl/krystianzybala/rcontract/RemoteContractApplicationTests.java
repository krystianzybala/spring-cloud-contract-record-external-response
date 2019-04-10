package pl.krystianzybala.rcontract;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.recording.RecordSpecBuilder;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureWireMock(port = 0)
@Slf4j
public class RemoteContractApplicationTests {

    @Rule
    public TestName testName = new TestName();

    @Autowired
    WireMockServer wireMockServer;

    private RestTemplate restTemplate;

    @Value("${wiremock.server.port}")
    private int wireMockPort;

    @Value("${pl.krystianzybala.stubs.target}")
    private String stubTarget;

    @Value("${pl.krystianzybala.stubs.ext}")
    private String stubExt;


    @Value("${pl.krystianzybala.wiremock.record.target}")
    private String wireMockRecordTarget;


    @Value("${pl.krystianzybala.proxy.baseUrl}")
    private String proxyBaseUrl;

    @Value("${pl.krystianzybala.proxy.endpoint}")
    private String proxyEndpoint;



    @Before
    public void setUp() {
        this.wireMockServer.startRecording(configure(WireMock.recordSpec()));

        this.restTemplate = new RestTemplateBuilder().rootUri(proxyBaseUrl + ":" + wireMockPort).build();
    }


    @After
    public void tearDown() {
        final SnapshotRecordResult snapshotRecordResult = this.wireMockServer.stopRecording();

        final List<StubMapping> stubMappings = snapshotRecordResult.getStubMappings();

        storeMapping(stubMappings);
    }

    private void storeMapping(final List<StubMapping> stubMappings) {

        log.info("Received stub Mappings: {}", stubMappings);


        try {
            final File target = new File(stubTarget);
            target.mkdir();

            for (final StubMapping stubMapping : stubMappings) {


                final File stub = new File(target, this.testName.getMethodName() + stubExt);
                stub.createNewFile();
                Files.write(stub.toPath(), stubMapping.toString().getBytes());
            }
        } catch (Exception exception) {
            log.error("Error occurred during saving mappings: {}", stubMappings, exception);
        }
    }

    @Test
    public void get_all_information_about_stations() {


        final ResponseEntity<String> response = this.restTemplate.getForEntity(proxyEndpoint, String.class);

        then(response.getStatusCodeValue())
                .isEqualTo(200);
        then(response.getBody())
                .isNotNull()
                .doesNotContain("HTTP Status 404 – Not Found");

        then(response.getBody())
                .isNotNull()
                .doesNotContain("HTTP Status 500 – Internal Server Error");
    }


    private RecordSpecBuilder configure(final RecordSpecBuilder recordSpec) {
        return recordSpec
                .forTarget(wireMockRecordTarget)
                .extractBinaryBodiesOver(99999L)
                .extractTextBodiesOver(99999L)
                .makeStubsPersistent(false);
    }

    @Test
    @Ignore
    public void contextLoads() {
    }

}
