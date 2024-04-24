package de.ukw.ccc.dnpmexport.mapper;

import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static de.ukw.ccc.dnpmexport.test.TestUtils.createKlinikAnamneseProcedure;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class KlinikAnamneseToDiagnoseMapperTest {

    private IOnkostarApi onkostarApi;
    private KlinikAnamneseToDiagnoseMapper mapper;

    @BeforeEach
    void setUp(
            @Mock IOnkostarApi onkostarApi
    ) {
        this.onkostarApi = onkostarApi;
        this.mapper = new KlinikAnamneseToDiagnoseMapper(new MapperUtils(onkostarApi));
    }

    @Test
    void shouldMapToDiagnose() {
        var procedure = createKlinikAnamneseProcedure(this.onkostarApi);
        procedure.setValue("ConsentStatusEinwilligungDNPM", new Item("ConsentStatusEinwilligungDNPM", "active"));
        procedure.setValue("DatumErstdiagnose", new Item("DatumErstdiagnose", new Date()));
        procedure.setValue("ICD10", new Item("ICD10", "F79.9"));
        procedure.setValue("ICDO3Lokalisation", new Item("ICDO3Lokalisation", "F79.2"));
        procedure.setValue("ICDO3Histologie", new Item("ICDO3Histologie", "8000/1"));
        procedure.setValue("WHOGrad", new Item("WHOGrad", "I"));

        var diagnose = this.mapper.apply(procedure);

        assertThat(diagnose).isNotEmpty();
        assertThat(diagnose.get().getIcd10().getCode()).isEqualTo("F79.9");
        assertThat(diagnose.get().getIcdO3T().getCode()).isEqualTo("F79.2");
    }

}
