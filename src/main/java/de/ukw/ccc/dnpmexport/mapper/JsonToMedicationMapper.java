/*
 * MIT License
 *
 * Copyright (c) 2024 Comprehensive Cancer Center Mainfranken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.ukw.ccc.dnpmexport.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ukw.ccc.bwhc.dto.Medication;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonToMedicationMapper implements Mapper<String, Optional<List<Medication>>> {

    private final ObjectMapper objectMapper;

    public JsonToMedicationMapper() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<List<Medication>> apply(String wirkstoffejson) {
        try {
            final var result = objectMapper.readValue(wirkstoffejson, new TypeReference<List<Wirkstoff>>() {
                    }).stream()
                    .map(wirkstoff -> Medication.builder()
                            .withCode(wirkstoff.code)
                            .withSystem(
                                    // Wirkstoff ohne Version => UNREGISTERED
                                    "ATC".equals(wirkstoff.system) && null != wirkstoff.version && !wirkstoff.version.isBlank()
                                            ? Medication.System.ATC
                                            : Medication.System.UNREGISTERED
                            )
                            .withVersion(wirkstoff.version)
                            .withDisplay(wirkstoff.name)
                            .build()
                    )
                    .collect(Collectors.toList());
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static class Wirkstoff {
        private String code;
        private String name;
        private String system;
        private String version;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
