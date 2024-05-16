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
import de.ukw.ccc.bwhc.dto.Diagnosis;
import de.ukw.ccc.bwhc.dto.Icd10;
import de.ukw.ccc.bwhc.dto.IcdO3T;
import de.ukw.ccc.bwhc.dto.WhoGrade;

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

        var datumErstdiagnose = procedure.getValue("DatumErstdiagnose").getDate();
        if (null != datumErstdiagnose) {
            builder.withRecordedOn(dateFormat().format(datumErstdiagnose));
        }

        var icd10 = procedure.getValue("ICD10").getString();
        if (null != icd10) {
            final var icd10Builder = Icd10.builder().withCode(icd10);
            mapperUtils.getSanitizedPropertyCatalogueVersionString(procedure.getValue("ICD10"))
                    .ifPresent(icd10Builder::withVersion);
            builder.withIcd10(icd10Builder.build());
        }

        var icd03T = procedure.getValue("ICDO3Lokalisation").getString();
        if (null != icd03T) {
            final var icdO3TBuilder = IcdO3T.builder().withCode(icd03T);
            mapperUtils.getSanitizedPropertyCatalogueVersionString(procedure.getValue("ICDO3Lokalisation"))
                    .ifPresent(icdO3TBuilder::withVersion);
            builder.withIcdO3T(icdO3TBuilder.build());
        }

        var whoGrade = mapWhoGrade(procedure.getValue("WHOGrad").getString());
        var whoGradeVersion = procedure.getValue("WHOGrad").getPropertyCatalogueVersion();
        if (null != whoGrade) {
            builder.withWhoGrade(WhoGrade.builder().withCode(whoGrade).withVersion(whoGradeVersion).build());
        }

        return Optional.of(builder.build());
    }

    private WhoGrade.WHOGrade mapWhoGrade(final String whoGradeValue) {
        switch (whoGradeValue) {
            case "I":
                return WhoGrade.WHOGrade.I;
            case "II":
                return WhoGrade.WHOGrade.II;
            case "III":
                return WhoGrade.WHOGrade.III;
            case "IV":
                return WhoGrade.WHOGrade.IV;
            default:
                return null;
        }
    }

}
