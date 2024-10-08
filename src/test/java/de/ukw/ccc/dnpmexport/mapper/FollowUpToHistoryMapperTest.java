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

import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Item;
import de.ukw.ccc.bwhc.dto.History;
import de.ukw.ccc.bwhc.dto.Medication;
import de.ukw.ccc.bwhc.dto.MolekularTherapyReasonStopped;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static de.ukw.ccc.dnpmexport.mapper.FollowUpToHistoryMapper.*;
import static de.ukw.ccc.dnpmexport.test.TestUtils.createEinzelempfehlungProcedure;
import static de.ukw.ccc.dnpmexport.test.TestUtils.createFollowUpProcedure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
public class FollowUpToHistoryMapperTest {

    private IOnkostarApi onkostarApi;
    private FollowUpToHistoryMapper mapper;

    @BeforeEach
    void setUp(
            @Mock IOnkostarApi onkostarApi
    ) {
        this.onkostarApi = onkostarApi;
        this.mapper = new FollowUpToHistoryMapper(new MapperUtils(onkostarApi));
    }

    @Test
    void shouldMapToHistory() throws Exception {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        var einzelempfehlung = createEinzelempfehlungProcedure(
                this.onkostarApi,
                procedure,
                List.of(Medication.builder().withCode("Test").withSystem(Medication.System.UNREGISTERED).build())
        );
        procedure.addSubProcedure("Einzelempfehlung", einzelempfehlung);

        doAnswer(invocationOnMock -> einzelempfehlung).when(onkostarApi).getProcedure(anyInt());

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getId()).matches("UNKNOWN[a-z0-9]+");
        assertThat(history.get().getPatient()).isEqualTo("2000123456");
        assertThat(history.get().getStatus()).isEqualTo(History.MolecularTherapyStatus.ON_GOING);
        assertThat(history.get().getBasedOn()).matches("UNKNOWN[a-z0-9]+");
    }

    @Test
    void shouldMapToHistoryWithMedication() throws Exception {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        var einzelempfehlung = createEinzelempfehlungProcedure(
                this.onkostarApi,
                procedure,
                List.of(Medication.builder().withCode("Test").withSystem(Medication.System.UNREGISTERED).build())
        );
        procedure.addSubProcedure("Einzelempfehlung", einzelempfehlung);

        doAnswer(invocationOnMock -> einzelempfehlung).when(onkostarApi).getProcedure(anyInt());

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getMedication()).hasSize(1);
        assertThat(history.get().getMedication().get(0)).isEqualTo(Medication.builder().withCode("Test").withSystem(Medication.System.UNREGISTERED).build());
    }

    @Test
    void shouldMapToHistoryWithPeriod() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        procedure.setValue(FIELD_NAME_PERIOD_START, new Item(FIELD_NAME_PERIOD_START, Date.from(Instant.parse("2024-05-12T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_PERIOD_END, new Item(FIELD_NAME_PERIOD_END, Date.from(Instant.parse("2024-05-13T12:00:00Z"))));

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getPeriod()).isNotNull();
        assertThat(history.get().getPeriod().getStart()).isEqualTo("2024-05-12");
        assertThat(history.get().getPeriod().getEnd()).isEqualTo("2024-05-13");
    }

    @Test
    void shouldMapToHistoryWithPeriodStartOnly() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        procedure.setValue(FIELD_NAME_PERIOD_START, new Item(FIELD_NAME_PERIOD_START, Date.from(Instant.parse("2024-05-12T12:00:00Z"))));

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getPeriod()).isNotNull();
        assertThat(history.get().getPeriod().getStart()).isEqualTo("2024-05-12");
        assertThat(history.get().getPeriod().getEnd()).isNull();
    }

    @Test
    void shouldMapToHistoryWithDosageLess50() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        procedure.setValue(FIELD_NAME_DOSAGE, new Item(FIELD_NAME_DOSAGE, "k"));

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getDosage()).isEqualTo(History.Dosage._50_);
        assertThat(history.get().getDosage().toString()).isEqualTo("<50%");
    }

    @Test
    void shouldMapToHistoryWithDosageGreaterEqual50() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        procedure.setValue(FIELD_NAME_DOSAGE, new Item(FIELD_NAME_DOSAGE, "g"));

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getDosage()).isEqualTo(History.Dosage._50);
        assertThat(history.get().getDosage().toString()).isEqualTo(">=50%");
    }

    @Test
    void shouldMapToHistoryWithReasonStopped() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        procedure.setValue(FIELD_NAME_REASON_STOPPED, new Item(FIELD_NAME_REASON_STOPPED, "pw"));

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getReasonStopped().getCode()).isEqualTo(MolekularTherapyReasonStopped.MolecularTherapyStopReason.PATIENT_WISH);
    }

    @Test
    void shouldMapToHistoryWithUnknownReasonStopped() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        procedure.setValue(FIELD_NAME_REASON_STOPPED, new Item(FIELD_NAME_REASON_STOPPED, "someunknownvalue"));

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getReasonStopped().getCode()).isEqualTo(MolekularTherapyReasonStopped.MolecularTherapyStopReason.UNKNOWN);
    }

    /// Onkostar property catalogue provides "Best Supportive Care" which is not included within data model
    @Test
    void shouldMapToHistoryWithOsPropertyCatalogueValueBscReasonStopped() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_STATUS, new Item(FIELD_NAME_STATUS, "on-going"));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        procedure.setValue(FIELD_NAME_REASON_STOPPED, new Item(FIELD_NAME_REASON_STOPPED, "bsc"));

        var history = this.mapper.apply(procedure);

        assertThat(history).isNotEmpty();
        assertThat(history.get().getReasonStopped().getCode()).isEqualTo(MolekularTherapyReasonStopped.MolecularTherapyStopReason.OTHER);
    }

    @Test
    void shouldNotMapToHistoryWithoutStatus() {
        var procedure = createFollowUpProcedure(this.onkostarApi);
        procedure.setValue(FIELD_NAME_RECORDED_ON, new Item(FIELD_NAME_RECORDED_ON, Date.from(Instant.parse("2024-05-14T12:00:00Z"))));
        procedure.setValue(FIELD_NAME_BASED_ON, new Item(FIELD_NAME_BASED_ON, "12345"));

        procedure.setValue(FIELD_NAME_REASON_STOPPED, new Item(FIELD_NAME_REASON_STOPPED, "pw"));

        var history = this.mapper.apply(procedure);

        assertThat(history).isEmpty();
    }

}
