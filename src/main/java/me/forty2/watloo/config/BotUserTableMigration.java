package me.forty2.watloo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Recreates bot_user table with updated UserState CHECK constraint (adds AWAITING_ITEM_*).
 * Needed when upgrading from a DB created before additem flow was added.
 */
@Slf4j
@Component
@Order(1)
public class BotUserTableMigration implements ApplicationRunner {

    private final DataSource dataSource;

    public BotUserTableMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String url = dataSource.getConnection().getMetaData().getURL();
        if (!url.contains("sqlite")) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("PRAGMA table_info(bot_user)");
            if (rows == null || rows.isEmpty()) {
                return;
            }
            // Try to insert a row with new state and rollback; if it fails, run migration
            try {
                jdbc.execute("INSERT INTO bot_user (id, user_state) VALUES (-1, 'AWAITING_ITEM_NAME')");
                jdbc.execute("DELETE FROM bot_user WHERE id = -1");
            } catch (Exception e) {
                log.info("bot_user table has old CHECK constraint, running migration to add new UserState values");
                runMigration(jdbc);
            }
        } catch (Exception e) {
            log.debug("BotUserTableMigration skip: {}", e.getMessage());
        }
    }

    private void runMigration(JdbcTemplate jdbc) {
        String createNew = """
                CREATE TABLE bot_user_new (
                    create_at timestamp,
                    id bigint not null,
                    bound_term varchar(255),
                    first_name varchar(255),
                    language_code varchar(255),
                    last_name varchar(255),
                    user_state varchar(255) check (user_state in (
                        'AWAITING_TERM_SELECTION','AWAITING_COURSE_NAME_INPUT','AWAITING_LOCATION_INPUT',
                        'AWAITING_DAY_SELECTION','AWAITING_TIME_INPUT','AWAITING_PROF_INPUT',
                        'AWAITING_RESTAURANT_REVIEW','AWAITING_ITEM_NAME','AWAITING_ITEM_CONDITION','AWAITING_ITEM_PRICE'
                    )),
                    username varchar(255),
                    primary key (id)
                )
                """;
        jdbc.execute(createNew);
        jdbc.execute("INSERT INTO bot_user_new SELECT create_at, id, bound_term, first_name, language_code, last_name, user_state, username FROM bot_user");
        jdbc.execute("DROP TABLE bot_user");
        jdbc.execute("ALTER TABLE bot_user_new RENAME TO bot_user");
        log.info("bot_user table migration completed");
    }
}
