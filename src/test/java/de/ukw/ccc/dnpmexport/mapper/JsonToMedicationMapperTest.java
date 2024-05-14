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

import de.ukw.ccc.bwhc.dto.Medication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class JsonToMedicationMapperTest {

    private JsonToMedicationMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new JsonToMedicationMapper();
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void shouldMapToMedication(String json, String code, String name, Medication.System system, String version) {
        final var medication = mapper.apply(json);

        assertThat(medication).isPresent();
        assertThat(medication.get().get(0).getCode()).isEqualTo(code);
        assertThat(medication.get().get(0).getDisplay()).isEqualTo(name);
        assertThat(medication.get().get(0).getSystem()).isEqualTo(system);
        assertThat(medication.get().get(0).getVersion()).isEqualTo(version);
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of("[{\"code\":\"B01AC06\", \"name\":\"Acetylsalicylsäure\", \"system\":\"ATC\", \"version\":\"\"}]", "B01AC06", "Acetylsalicylsäure", Medication.System.UNREGISTERED, ""),
                Arguments.of("[{\"code\":\"ASS\", \"name\":\"Acetylsalicylsäure\", \"system\":\"\", \"version\":\"\"}]", "ASS", "Acetylsalicylsäure", Medication.System.UNREGISTERED, ""),
                Arguments.of("[{\"code\":\"B01AC06\", \"name\":\"Acetylsalicylsäure\", \"system\":\"\", \"version\":\"2024\"}]", "B01AC06", "Acetylsalicylsäure", Medication.System.UNREGISTERED, "2024"),
                Arguments.of("[{\"code\":\"B01AC06\", \"name\":\"Acetylsalicylsäure\", \"system\":\"ATC\", \"version\":\"2024\"}]", "B01AC06", "Acetylsalicylsäure", Medication.System.ATC, "2024")
        );
    }

}
