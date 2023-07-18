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

import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Procedure;
import de.ukw.ccc.bwhc.dto.FamilyMemberDiagnosis;
import de.ukw.ccc.bwhc.dto.Relationship;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KlinikAnamneseToFamilyMemberDiagnosisMapper implements Function<Procedure, List<FamilyMemberDiagnosis>> {

    private final IOnkostarApi onkostarApi;

    public KlinikAnamneseToFamilyMemberDiagnosisMapper(final IOnkostarApi onkostarApi) {
        this.onkostarApi = onkostarApi;
    }

    @Override
    public List<FamilyMemberDiagnosis> apply(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("DNPM Klinik/Anamnese")) {
            return List.of();
        }

        return onkostarApi
                .getProceduresForDiseaseByForm(procedure.getDiseaseIds().get(0), "DNPM UF Verwandte")
                .stream()
                .filter(p -> p.getParentProcedureId() == procedure.getId())
                .map(p ->
                        FamilyMemberDiagnosis.builder()
                                .withId(p.getId().toString())
                                .withPatient(procedure.getPatient().getId().toString())
                                .withRelationship(getRelationship(p))
                                .build()
                ).collect(Collectors.toList());

    }

    private Relationship getRelationship(Procedure procedure) {
        var grad = procedure.getValue("Verwandtschaftsgrad").getString();
        if (grad.equals("FAMMEMB")) {
            return Relationship.builder().withCode(Relationship.RelationshipCode.FAMMEMB).build();
        }
        return Relationship.builder().withCode(Relationship.RelationshipCode.EXT).build();
    }

}
