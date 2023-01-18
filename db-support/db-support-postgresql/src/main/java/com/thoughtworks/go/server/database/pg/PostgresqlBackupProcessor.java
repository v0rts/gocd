/*
 * Copyright 2023 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.database.pg;

import com.thoughtworks.go.server.database.BackupProcessor;
import com.thoughtworks.go.server.database.DbProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.tools.ant.types.Commandline;
import org.postgresql.Driver;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessInitException;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import javax.sql.DataSource;
import java.io.File;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class PostgresqlBackupProcessor implements BackupProcessor {

    private static final String COMMAND = "pg_dump";

    @Override
    public void backup(File targetDir, DataSource dataSource, DbProperties dbProperties) throws Exception {
        try {
            ProcessResult processResult = createProcessExecutor(targetDir, dbProperties).execute();

            if (processResult.getExitValue() == 0) {
                log.info("PostgreSQL backup finished successfully.");
            } else {
                throwBackupError(COMMAND, processResult.getExitValue());
            }
        } catch (ProcessInitException e) {
            throwBackupError(COMMAND, e.getErrorCode(), e.getCause());
        }
    }

    @Override
    public boolean accepts(String url) {
        return isNotBlank(url) && url.startsWith("jdbc:postgresql:");
    }

    ProcessExecutor createProcessExecutor(File targetDir, DbProperties dbProperties) {
        Properties connectionProperties = dbProperties.connectionProperties();
        Properties pgProperties = Driver.parseURL(dbProperties.url(), connectionProperties);

        Map<String, String> env = new LinkedHashMap<>();
        if (isNotBlank(dbProperties.password())) {
            env.put("PGPASSWORD", dbProperties.password());
        }

        // override with any user specified environment
        env.putAll(dbProperties.extraBackupEnv());

        List<String> argv = new ArrayList<>();
        argv.add(COMMAND);

        String dbName = pgProperties.getProperty("PGDBNAME");
        argv.add("--host=" + pgProperties.getProperty("PGHOST"));
        argv.add("--port=" + pgProperties.getProperty("PGPORT"));
        argv.add("--dbname=" + dbName);
        if (isNotBlank(dbProperties.user())) {
            argv.add("--username=" + dbProperties.user());
        }
        argv.add("--no-password");
        // append any user specified args for pg_dump
        if (isNotBlank(dbProperties.extraBackupCommandArgs())) {
            Collections.addAll(argv, Commandline.translateCommandline(dbProperties.extraBackupCommandArgs()));
        }
        argv.add("--file=" + new File(targetDir, "db." + dbName));
        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.redirectOutputAlsoTo(Slf4jStream.of(getClass()).asInfo());
        processExecutor.redirectErrorAlsoTo(Slf4jStream.of(getClass()).asWarn());
        processExecutor.environment(env);
        processExecutor.command(argv);
        return processExecutor;
    }
}
