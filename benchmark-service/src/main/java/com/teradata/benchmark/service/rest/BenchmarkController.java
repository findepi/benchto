/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.rest;

import com.teradata.benchmark.service.BenchmarkService;
import com.teradata.benchmark.service.model.Benchmark;
import com.teradata.benchmark.service.model.BenchmarkRun;
import com.teradata.benchmark.service.model.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class BenchmarkController
{

    @Autowired
    private BenchmarkService benchmarkService;

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/start", method = POST)
    public void startBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId)
    {
        benchmarkService.startBenchmarkRun(benchmarkName, benchmarkSequenceId);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/finish", method = POST)
    public void finishBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @RequestBody List<Measurement> measurements)
    {
        benchmarkService.finishBenchmarkRun(benchmarkName, benchmarkSequenceId, measurements);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", method = POST)
    public void startExecution(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @PathVariable("executionSequenceId") String executionSequenceId)
    {
        benchmarkService.startExecution(benchmarkName, benchmarkSequenceId, executionSequenceId);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", method = POST)
    public void finishExecution(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @PathVariable("executionSequenceId") String executionSequenceId,
            @RequestBody List<Measurement> measurements)
    {
        benchmarkService.finishExecution(benchmarkName, benchmarkSequenceId, executionSequenceId, measurements);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}", method = GET)
    public BenchmarkRun findBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId)
    {
        return benchmarkService.findBenchmarkRun(benchmarkName, benchmarkSequenceId);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}", method = GET)
    public Benchmark findBenchmarks(
            @PathVariable("benchmarkName") String benchmarkName,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(value = "from") Optional<ZonedDateTime> from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(value = "to") Optional<ZonedDateTime> to,
            Pageable pageable)
    {
        return benchmarkService.findBenchmark(benchmarkName, from, to, pageable);
    }

    @RequestMapping(value = "/v1/benchmark/latest", method = GET)
    public List<BenchmarkRun> findLatestBenchmarkRuns(Pageable pageable)
    {
        return benchmarkService.findLatest(pageable);
    }
}
