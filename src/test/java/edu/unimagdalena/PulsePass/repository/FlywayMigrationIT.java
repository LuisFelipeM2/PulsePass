package edu.unimagdalena.PulsePass.repository;

import edu.unimagdalena.PulsePass.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
 
import static org.assertj.core.api.Assertions.assertThat;
 
// QT-001/002 (seccion 17) / NFR-002/003 (seccion 12): verifica que Flyway
// aplique V1, V2 y V3 desde un PostgreSQL vacio (levantado por Testcontainers
// en cada corrida, demostrando que el esquema es 100% reconstruible), y que
// Hibernate en modo 'validate' no encuentre discrepancias con las entidades.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class FlywayMigrationIT {
 
    @Autowired
    private JdbcTemplate jdbcTemplate;
 
    // Comprueba que las 3 migraciones quedaron registradas como exitosas
    // en la tabla de control propia de Flyway.
    @Test
    void deberiaAplicarLasTresMigracionesExitosamente() {
        Integer countAplicadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );
 
        assertThat(countAplicadas).isEqualTo(3); // V1, V2, V3
    }
 
    // Comprueba que las 7 tablas del esquema relacional (seccion 7 del PRD)
    // existen realmente en PostgreSQL, no solo en el codigo Java.
    @Test
    void deberianExistirLasSieteTablasEsperadas() {
        Integer countTablas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                AND table_name IN ('venues','events','artists','event_artists',
                                    'users','user_profiles','tickets')
                """, Integer.class);
 
        assertThat(countTablas).isEqualTo(7);
    }
 
    // Comprueba que V2 realmente inserto los 5 artistas semilla.
    @Test
    void deberiaExistirElCatalogoInicialDeArtistas() {
        Integer countArtistas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM artists", Integer.class
        );
 
        assertThat(countArtistas).isEqualTo(5);
    }
 
    // Comprueba que V3 agrego la columna streaming_url a events.
    @Test
    void deberiaExistirLaColumnaStreamingUrlEnEvents() {
        Integer countColumna = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'events' AND column_name = 'streaming_url'
                """, Integer.class);
 
        assertThat(countColumna).isEqualTo(1);
    }
}
