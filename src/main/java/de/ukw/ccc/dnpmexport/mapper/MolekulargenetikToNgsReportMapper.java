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
import de.ukw.ccc.bwhc.dto.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MolekulargenetikToNgsReportMapper extends MolekulargenetikMapper<Optional<NgsReport>> {

    public MolekulargenetikToNgsReportMapper(final MapperUtils mapperUtils) {
        super(mapperUtils);
    }

    @Override
    public Optional<NgsReport> apply(Procedure procedure) {
        if (!canApply(procedure)) {
            return Optional.empty();
        }

        // Only one diagnosis!
        if (procedure.getDiseases().size() != 1) {
            return Optional.empty();
        }

        // Erweiterte Dokumentation erforderlich
        if (!"ERW".equals(procedure.getValue("Dokumentation").getString())) {
            return Optional.empty();
        }

        var builder = NgsReport.builder()
                .withId(anonymizeId(procedure))
                .withPatient(getPatientId(procedure))
                .withIssueDate(dateFormat().format(procedure.getStartDate()))
                .withSpecimen(anonymizeId(procedure))
                .withSequencingType(mapArtDerSequenzierung(procedure.getValue("ArtDerSequenzierung").getString()))
                /*.withTumorCellContent(
                        TumorCellContent.builder()
                                .withId(anonymizeId(procedure))
                                .withSpecimen(anonymizeId(procedure))
                                .withMethod(TumorCellContent.ObservationMethod.BIOINFORMATIC)
                                .withValue(procedure.getValue("Tumorzellgehalt").getDouble())
                                .build()
                )*/
                ;

        builder
                .withSimpleVariants(getSimpleVariants(procedure));

        return Optional.of(builder.build());
    }

    private List<SimpleVariant> getSimpleVariants(Procedure procedure) {
        return this.mapperUtils.onkostarApi()
                .getProceduresForDiseaseByForm(procedure.getDiseaseIds().get(0), "OS.Molekulargenetische Untersuchung")
                .stream()
                .filter(p -> p.getParentProcedureId() == procedure.getId())
                // Einfache Variante
                .filter(p -> p.getValue("Ergebnis").getString().equals("P"))
                .map(
                        p -> {
                            var builder = SimpleVariant.builder().withId(anonymizeId(p));
                            builder.withChromosome(
                                            p.getValue("EVChromosom").getString()
                                    )
                                    .withStartEnd(
                                            StartEnd.builder()
                                                    .withStart(p.getValue("EVStart").getDouble())
                                                    .withEnd(p.getValue("EVEnde").getDouble())
                                                    .build()
                                    )
                                    .withRefAllele(
                                            p.getValue("EVRefNucleotide").getString()
                                    )
                                    .withAltAllele(
                                            p.getValue("EVAltNucleotide").getString()
                                    )
                                    .withDnaChange(
                                            // Todo Sufficient?
                                            DnaChange.builder().withCode(p.getValue("cDNANomenklatur").getString()).build()
                                    )
                                    .withAminoAcidChange(
                                            // Todo Sufficient?
                                            AminoAcidChange.builder().withCode(p.getValue("ProteinebeneNomenklatur").getString()).build()
                                    )
                                    .withReadDepth(
                                            p.getValue("EVReadDepth").getInt()
                                    )
                                    .withAllelicFrequency(
                                            p.getValue("Allelfrequenz").getDouble()
                                    )
                                    .withCosmicId(
                                            p.getValue("EVCOSMICID").getString()
                                    )
                                    .withDbSNPId(
                                            p.getValue("EVdbSNPID").getString()
                                    )
                                    .withInterpretation(
                                            Interpretation.builder().withCode(p.getValue("Pathogenitaetsklasse").getString()).build()
                                    );
                            return builder.build();
                        }
                ).collect(Collectors.toList());


    }

    private String mapArtDerSequenzierung(String value) {
        switch (value) {
            case "PanelKit":
                return "Panel";
            default:
                return value;
        }
    }

}
