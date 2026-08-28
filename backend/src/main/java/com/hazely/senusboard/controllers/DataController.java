package com.hazely.senusboard.controllers;

import com.hazely.senusboard.dtos.DataDto;
import com.hazely.senusboard.dtos.DocumentDownloadDto;
import com.hazely.senusboard.dtos.DocumentsDto;
import com.hazely.senusboard.dtos.ReportingPeriodsDto;
import com.hazely.senusboard.services.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** Exposes reporting-period financial data. */
@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService service;

    /** Returns the complete dataset for the requested period. */
    @GetMapping("/{period}")
    public DataDto getData(@PathVariable String period) {
        return service.getData(period);
    }

    /** Returns all available reporting periods. */
    @GetMapping("/reporting-periods")
    public ReportingPeriodsDto getPeriods() {
        return service.getPeriods();
    }

    /** Returns source-document metadata and available download links. */
    @GetMapping("/documents")
    public DocumentsDto getDocuments() {
        return service.getDocuments();
    }

    /** Downloads one locally available source document. */
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentDownloadDto data = service.getDownload(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(data.name(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(data.mediaType())
                .contentLength(data.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(data.resource());
    }
}
