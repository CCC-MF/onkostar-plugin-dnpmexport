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

import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.*;

import java.util.Optional;

public class KlinikAnamneseToDiagnoseMapper extends KlinikAnamneseMapper<Optional<Diagnosis>> {

    public KlinikAnamneseToDiagnoseMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public Optional<Diagnosis> apply(Procedure procedure) {
        if (!canApply(procedure)) {
            return Optional.empty();
        }

        var builder = Diagnosis.builder()
                .withId(mapperUtils.anonymizeId(procedure.getId().toString()))
                .withPatient(getPatientId(procedure));

        var datumErstiagnose = procedure.getValue("DatumErstdiagnose").getDate();
        if (null != datumErstiagnose) {
            builder.withRecordedOn(dateFormat().format(datumErstiagnose));
        }

        var icd10 = procedure.getValue("ICD10").getString();
        var icd10Version = procedure.getValue("ICD10").getPropertyCatalogueVersion();
        if (null != icd10) {
            builder.withIcd10(Icd10.builder().withCode(icd10).withVersion(icd10Version).build());
        }

        var icd03T = procedure.getValue("ICDO3Lokalisation").getString();
        var icd03TVersion = procedure.getValue("ICDO3Lokalisation").getPropertyCatalogueVersion();
        if (null != icd03T) {
            builder.withIcdO3T(IcdO3T.builder().withCode(icd03T).withVersion(icd03TVersion).build());
        }

        var whoGrade = procedure.getValue("WHOGrad").getString();
        var whoGradeVersion = procedure.getValue("WHOGrad").getPropertyCatalogueVersion();
        if (null != whoGrade) {
            // Todo: Mapping Grading I-IV
            //builder.withWhoGrade(WhoGrade.builder().withCode(...).withVersion(whoGradeVersion).build());
        }

        return Optional.of(builder.build());
    }

}
