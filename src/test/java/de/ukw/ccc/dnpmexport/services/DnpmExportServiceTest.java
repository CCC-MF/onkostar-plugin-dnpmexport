/*
 * This file is part of ETL-Processor
 *
 * Copyright (C) 2024  Comprehensive Cancer Center Mainfranken, Datenintegrationszentrum Philipps-Universität Marburg and Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package de.ukw.ccc.dnpmexport.services;

import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Date;

import static de.ukw.ccc.dnpmexport.test.TestUtils.createKlinikAnamneseProcedure;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DnpmExportServiceTest {

    private IOnkostarApi onkostarApi;

    private RestTemplate restTemplate;

    private DnpmExportService dnpmExportService;

    @BeforeEach
    void setup(
            @Mock IOnkostarApi onkostarApi,
            @Mock RestTemplate restTemplate
    ) {
        this.onkostarApi = onkostarApi;
        this.restTemplate = restTemplate;
        this.dnpmExportService = new DnpmExportService(onkostarApi, restTemplate);
    }

    private static String defaultSetting(String name) {
        switch (name) {
            case "dnpmexport_url":
                return "http://example.com/mtbfile";
            case "dnpmexport_prefix":
                return "TEST";
            case "dnpmexport_export_consent_rejected":
                return "false";
            default:
                return null;
        }
    }

    @Test
    void shouldExportMtbFileWithConsentActive() {
        doAnswer(invocationOnMock -> {
            var name = invocationOnMock.getArgument(0, String.class);
            return defaultSetting(name);
        }).when(this.onkostarApi).getGlobalSetting(anyString());

        var procedure = createKlinikAnamneseProcedure(this.onkostarApi);
        procedure.setValue("ConsentStatusEinwilligungDNPM", new Item("ConsentStatusEinwilligungDNPM", "active"));
        procedure.setValue("DatumErstdiagnose", new Item("DatumErstdiagnose", new Date()));
        procedure.setValue("ICD10", new Item("ICD10", "F79.9"));
        procedure.setValue("ICDO3Histologie", new Item("ICDO3Histologie", "8000/1"));
        procedure.setValue("WHOGrad", new Item("WHOGrad", "I"));

        when(this.restTemplate.postForEntity(any(URI.class), any(), any())).thenReturn(ResponseEntity.accepted().build());

        this.dnpmExportService.export(procedure);

        verify(restTemplate, times(1)).postForEntity(any(URI.class), any(), any());
    }

    @Test
    void shouldExportMtbFileWithConsentRejectedIfRequired() {
        doAnswer(invocationOnMock -> {
            var name = invocationOnMock.getArgument(0, String.class);
            if (name.equals("dnpmexport_export_consent_rejected")) {
                return "true";
            }
            return defaultSetting(name);
        }).when(this.onkostarApi).getGlobalSetting(anyString());

        var procedure = createKlinikAnamneseProcedure(this.onkostarApi);
        procedure.setValue("ConsentStatusEinwilligungDNPM", new Item("ConsentStatusEinwilligungDNPM", "active"));
        procedure.setValue("DatumErstdiagnose", new Item("DatumErstdiagnose", new Date()));
        procedure.setValue("ICD10", new Item("ICD10", "F79.9"));
        procedure.setValue("ICDO3Histologie", new Item("ICDO3Histologie", "8000/1"));
        procedure.setValue("WHOGrad", new Item("WHOGrad", "I"));

        when(this.restTemplate.postForEntity(any(URI.class), any(), any())).thenReturn(ResponseEntity.accepted().build());

        this.dnpmExportService.export(procedure);

        verify(restTemplate, times(1)).postForEntity(any(URI.class), any(), any());
    }

    @Test
    void shouldSendDeleteRequestWithConsentRejected() {
        doAnswer(invocationOnMock -> {
            var name = invocationOnMock.getArgument(0, String.class);
            return defaultSetting(name);
        }).when(this.onkostarApi).getGlobalSetting(anyString());

        when(this.restTemplate.exchange(any(URI.class), any(), any(), any(Class.class))).thenReturn(ResponseEntity.accepted().build());

        var procedure = createKlinikAnamneseProcedure(this.onkostarApi);
        this.dnpmExportService.export(procedure);

        var captor = ArgumentCaptor.forClass(HttpMethod.class);
        verify(restTemplate, times(1)).exchange(any(URI.class), captor.capture(), any(), any(Class.class));
    }

}
