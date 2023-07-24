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
import de.ukw.ccc.bwhc.dto.*;

import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.ukw.ccc.dnpmexport.mapper.MapperUtils.getPatientId;

public class MolekulargenetikToSpecimenMapper implements Function<Procedure, Optional<Specimens>> {

    private final IOnkostarApi onkostarApi;

    private final MapperUtils mapperUtils;

    public MolekulargenetikToSpecimenMapper(final IOnkostarApi onkostarApi) {
        this.onkostarApi = onkostarApi;
        this.mapperUtils = new MapperUtils(onkostarApi);
    }

    @Override
    public Optional<Specimens> apply(Procedure procedure) {
        if (null == procedure || !procedure.getFormName().equals("OS.Molekulargenetik")) {
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

        var builder = Specimens.builder()
                .withId(mapperUtils.anonymizeId(procedure.getId().toString()))
                .withPatient(getPatientId(procedure));

        var entnahmedatum = procedure.getValue("Entnahmedatum").getString();
        var entnahmemethode = procedure.getValue("Entnahmemethode").getString();
        if ("LB".equals(entnahmemethode)) {
            builder.withType(Specimens.Type.LIQUID_BIOPSY);
        } else {
            var materialfixierung = procedure.getValue("Materialfixierung").getString();
            builder.withType(mapMaterialfixierungType(materialfixierung));
        }

        var propenmaterial = procedure.getValue("Probenmaterial").getString();
        builder.withCollection(
                Collection.builder()
                        .withDate(entnahmedatum)
                        .withMethod(mapMaterialfixierungMethod(entnahmemethode))
                        .withLocalization(mapProbenmaterialLocalization(propenmaterial))
                        .build()
        );

        mapperUtils.getIcd10(procedure.getDiseases().get(0)).ifPresent(builder::withIcd10);

        return Optional.of(builder.build());
    }

    private Specimens.Type mapMaterialfixierungType(String type) {
        switch (type) {
            case "2":
                return Specimens.Type.CRYO_FROZEN;
            case "3":
                return Specimens.Type.FFPE;
            default:
                return Specimens.Type.UNKNOWN;
        }
    }

    private Collection.Method mapMaterialfixierungMethod(String type) {
        switch (type) {
            case "B":
                return Collection.Method.BIOPSY;
            case "R":
                return Collection.Method.RESECTION;
            case "LB":
                return Collection.Method.LIQUID_BIOPSY;
            case "Z":
                return Collection.Method.CYTOLOGY;
            default:
                return Collection.Method.UNKNOWN;
        }
    }

    private Collection.Localization mapProbenmaterialLocalization(String probenmaterial) {
        switch (probenmaterial) {
            case "T":
                return Collection.Localization.PRIMARY_TUMOR;
            case "LK":
            case "M":
            case "ITM":
            case "SM":
            case "KM":
                return Collection.Localization.METASTASIS;
            default:
                return Collection.Localization.UNKNOWN;
        }
    }

}
