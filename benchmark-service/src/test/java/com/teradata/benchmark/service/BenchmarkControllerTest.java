/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service;

import com.teradata.benchmark.service.category.IntegrationTest;
import com.teradata.benchmark.service.model.BenchmarkRun;
import com.teradata.benchmark.service.model.BenchmarkRunExecution;
import com.teradata.benchmark.service.repo.BenchmarkRunRepo;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.teradata.benchmark.service.model.MeasurementUnit.BYTES;
import static com.teradata.benchmark.service.model.MeasurementUnit.MILLISECONDS;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Category(IntegrationTest.class)
public class BenchmarkControllerTest
        extends IntegrationTestBase
{

    @Autowired
    private BenchmarkRunRepo benchmarkRunRepo;

    @Test
    public void benchmarkStartEndHappyPath()
            throws Exception
    {
        String benchmarkName = "benchmarkName";
        String benchmarkSequenceId = "benchmarkSequenceId";
        String executionSequenceId = "executionSequenceId";
        ZonedDateTime testStart = nowInUtc();

        // start benchmark
        mvc.perform(post("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/start", benchmarkName, benchmarkSequenceId))
                .andExpect(status().isOk());

        // get benchmark - no measurements, no executions
        mvc.perform(get("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}", benchmarkName, benchmarkSequenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(benchmarkName)))
                .andExpect(jsonPath("$.sequenceId", is(benchmarkSequenceId)))
                .andExpect(jsonPath("$.measurements", hasSize(0)))
                .andExpect(jsonPath("$.executions", hasSize(0)));

        // start execution
        mvc.perform(post("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start",
                benchmarkName, benchmarkSequenceId, executionSequenceId))
                .andExpect(status().isOk());

        // get benchmark - no measurements, single execution without measurements
        mvc.perform(get("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}", benchmarkName, benchmarkSequenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(benchmarkName)))
                .andExpect(jsonPath("$.sequenceId", is(benchmarkSequenceId)))
                .andExpect(jsonPath("$.measurements", hasSize(0)))
                .andExpect(jsonPath("$.executions", hasSize(1)))
                .andExpect(jsonPath("$.executions[0].sequenceId", is(executionSequenceId)));

        // finish execution - post execution measurements
        mvc.perform(post("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish",
                benchmarkName, benchmarkSequenceId, executionSequenceId)
                .contentType(APPLICATION_JSON)
                .content("[{\"name\": \"duration\", \"value\": 12.34, \"unit\": \"MILLISECONDS\"},{\"name\": \"bytes\", \"value\": 56789.0, \"unit\": \"BYTES\"}]"))
                .andExpect(status().isOk());

        // finish benchmark - post benchmark measurements
        mvc.perform(post("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/finish", benchmarkName, benchmarkSequenceId)
                .contentType(APPLICATION_JSON)
                .content("[{\"name\": \"meanDuration\", \"value\": 12.34, \"unit\": \"MILLISECONDS\"},{\"name\": \"sumBytes\", \"value\": 56789.0, \"unit\": \"BYTES\"}]"))
                .andExpect(status().isOk());

        ZonedDateTime testEnd = nowInUtc();

        // get benchmark runs in given time range - measurements, single execution with measurements
        mvc.perform(get("/v1/benchmark/{benchmarkName}?from={from}&to={to}",
                benchmarkName, testStart.format(ISO_DATE_TIME), nowInUtc().format(ISO_DATE_TIME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(benchmarkName)))
                .andExpect(jsonPath("$.runs[0].sequenceId", is(benchmarkSequenceId)))
                .andExpect(jsonPath("$.runs[0].measurements", hasSize(2)))
                .andExpect(jsonPath("$.runs[0].measurements[*].name", containsInAnyOrder("meanDuration", "sumBytes")))
                .andExpect(jsonPath("$.runs[0].measurements[*].value", containsInAnyOrder(12.34, 56789.0)))
                .andExpect(jsonPath("$.runs[0].measurements[*].unit", containsInAnyOrder("MILLISECONDS", "BYTES")))
                .andExpect(jsonPath("$.runs[0].executions", hasSize(1)))
                .andExpect(jsonPath("$.runs[0].executions[0].sequenceId", is(executionSequenceId)))
                .andExpect(jsonPath("$.runs[0].executions[0].measurements[*].name", containsInAnyOrder("duration", "bytes")))
                .andExpect(jsonPath("$.runs[0].executions[0].measurements[*].value", containsInAnyOrder(12.34, 56789.0)))
                .andExpect(jsonPath("$.runs[0].executions[0].measurements[*].unit", containsInAnyOrder("MILLISECONDS", "BYTES")));

        // check no benchmarks stored before starting test
        mvc.perform(get("/v1/benchmark/{benchmarkName}?from={from}&to={to}",
                benchmarkName, testStart.minusHours(1).format(ISO_DATE_TIME), testStart.format(ISO_DATE_TIME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(benchmarkName)))
                .andExpect(jsonPath("$.runs", hasSize(0)));

        // assert database state
        withinTransaction(() -> {
            BenchmarkRun benchmarkRun = benchmarkRunRepo.findByNameAndSequenceId(benchmarkName, benchmarkSequenceId);
            assertThat(benchmarkRun).isNotNull();
            assertThat(benchmarkRun.getId()).isGreaterThan(0);
            assertThat(benchmarkRun.getName()).isEqualTo(benchmarkName);
            assertThat(benchmarkRun.getSequenceId()).isEqualTo(benchmarkSequenceId);
            assertThat(benchmarkRun.getMeasurements())
                    .hasSize(2)
                    .extracting("unit").contains(BYTES, MILLISECONDS);
            assertThat(benchmarkRun.getStarted())
                    .isAfter(testStart)
                    .isBefore(testEnd);
            assertThat(benchmarkRun.getEnded())
                    .isAfter(testStart)
                    .isBefore(testEnd);
            assertThat(benchmarkRun.getExecutions())
                    .hasSize(1);

            BenchmarkRunExecution execution = benchmarkRun.getExecutions().iterator().next();
            assertThat(execution.getId()).isGreaterThan(0);
            assertThat(execution.getSequenceId()).isEqualTo(executionSequenceId);
            assertThat(execution.getMeasurements())
                    .hasSize(2)
                    .extracting("name").contains("duration", "bytes");
            assertThat(execution.getStarted())
                    .isAfter(testStart)
                    .isBefore(testEnd);
            assertThat(execution.getEnded())
                    .isAfter(testStart)
                    .isBefore(testEnd);
        });
    }

    private ZonedDateTime nowInUtc()
    {
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }
}
