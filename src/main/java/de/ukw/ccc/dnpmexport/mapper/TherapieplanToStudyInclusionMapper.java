/*
 * MIT License
 *
 * Copyright (c) 2023 Comprehensive Cancer Center Mainfranken
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.StudyInclusionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class TherapieplanToStudyInclusionMapper extends TherapieplanMapper<List<StudyInclusionRequest>> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper objectMapper;

    public TherapieplanToStudyInclusionMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<StudyInclusionRequest> apply(Procedure procedure) {
        if (!canApply(procedure)) {
            return List.of();
        }

        // Only one diagnosis!
        if (procedure.getDiseases().size() != 1) {
            return List.of();
        }

        return mapperUtils.onkostarApi()
                .getProceduresForDiseaseByForm(procedure.getDiseaseIds().get(0), "DNPM UF Einzelempfehlung")
                .stream()
                .filter(p -> p.getParentProcedureId() == procedure.getId())
                .flatMap(p -> nctNumbers(p).stream().map(nctNumber -> StudyInclusionRequest.builder()
                        .withId(mapperUtils.anonymizeId(procedure.getId().toString() + "_" + nctNumber))
                        .withPatient(getPatientId(procedure))
                        .withReason(mapperUtils.anonymizeId(procedure.getDiseaseIds().get(0).toString()))
                        .withIssuedOn(issuedOn(p))
                        .withNctNumber(nctNumber)
                        .build())
                )
                .collect(Collectors.toList());
    }

    private String issuedOn(Procedure procedure) {
        return mapperUtils.einzelempfehlungMtbDate(procedure);
    }

    private List<String> nctNumbers(Procedure procedure) {
        var json = procedure.getValue("studienallejson").getString();

        if (json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper
                    .readValue(json, new TypeReference<List<Studie>>() {
                    })
                    .stream()
                    .map(s -> s.nct)
                    .filter(nct -> nct.toUpperCase().startsWith("NCT"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("JSON Parse Error: Not a Study with NCT number", e);
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Studie {
        private String nct;

        public String getNct() {
            return nct;
        }

        public void setNct(String nct) {
            this.nct = nct;
        }
    }

}
