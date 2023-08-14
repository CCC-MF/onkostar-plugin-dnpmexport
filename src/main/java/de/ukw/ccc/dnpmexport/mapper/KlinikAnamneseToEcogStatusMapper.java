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

import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.EcogStatusValue;
import de.ukw.ccc.bwhc.dto.Ecogstatus;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.ukw.ccc.dnpmexport.mapper.MapperUtils.getPatientId;

public class KlinikAnamneseToEcogStatusMapper extends ProcedureMapper<List<Ecogstatus>> {

    public KlinikAnamneseToEcogStatusMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public List<Ecogstatus> apply(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Klinik/Anamnese")) {
            return List.of();
        }

        return mapperUtils.onkostarApi()
                .getProceduresForDiseaseByForm(procedure.getDiseaseIds().get(0), "DNPM UF ECOG")
                .stream()
                .filter(p -> p.getParentProcedureId() == procedure.getId())
                .map(p -> {
                    if (null != getStatus(p)) {
                        return Ecogstatus.builder()
                                .withId(anonymizeId(p))
                                .withPatient(getPatientId(procedure))
                                .withEffectiveDate(p.getValue("Datum").getString())
                                .withValue(getStatus(p))
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }

    private EcogStatusValue getStatus(Procedure procedure) {
        var ecog = procedure.getValue("ECOG").getString();
        switch (ecog) {
            case "0":
                return EcogStatusValue.builder().withCode(EcogStatusValue.Ecog._0).build();
            case "1":
                return EcogStatusValue.builder().withCode(EcogStatusValue.Ecog._1).build();
            case "2":
                return EcogStatusValue.builder().withCode(EcogStatusValue.Ecog._2).build();
            case "3":
                return EcogStatusValue.builder().withCode(EcogStatusValue.Ecog._3).build();
            case "4":
                return EcogStatusValue.builder().withCode(EcogStatusValue.Ecog._4).build();
            default:
                return null;
        }
    }

}
