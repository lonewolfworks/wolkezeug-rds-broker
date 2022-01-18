package com.lonewolfworks.wolke.rds.credentialbroker.broker

import com.lonewolfworks.wolke.rds.credentialbroker.config.DataSourceProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.DriverManager
import java.util.*

/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Component
@Profile("mysqleight")
open class Mysql80CredentialBroker(
        private val template: JdbcTemplate,
        private val dataSourceProperties: DataSourceProperties,
        @Qualifier("masterCredential")
        private val masterCredential: Credential,
        @Qualifier("appCredential")
        private val appCredential: Credential,
        @Qualifier("adminCredential")
        private val adminCredential: Credential
) : CredentialBroker {
    private val LOG = LoggerFactory.getLogger(Mysql80CredentialBroker::class.java)

    override fun brokerCredentials() {
        setUpUser(appCredential)
        grantAppAccess(appCredential)
        testCredential(appCredential)

        setUpUser(adminCredential)
        grantAdminAccess(adminCredential)
        testCredential(adminCredential)

        grantAdminAccess(masterCredential)
    }

    private fun setUpUser(credential: Credential) {
        LOG.info("Setting up user: {}", credential.username)
        dropUserIfExists(credential)
        createUser(credential)

    }

    private fun dropUserIfExists(credential: Credential) {
        val grantQuery = String.format("GRANT USAGE ON *.* TO %s;", credential.username)
        val dropQuery = String.format("DROP USER %s;", credential.username)
        template.execute(grantQuery)
        template.execute(dropQuery)
    }

    private fun createUser(credential: Credential) {
        val query = String.format("CREATE USER '%s' IDENTIFIED BY '%s';", credential.username, credential.password)
        template.execute(query)
    }

    private fun grantAppAccess(credential: Credential) {
        val grantQuery = String.format("GRANT SELECT, INSERT, UPDATE, DELETE ON %s.* TO '%s'@'%%';", dataSourceProperties.databaseName, credential.username, credential.password)
        val flushQuery = "FLUSH PRIVILEGES;"
        template.execute(grantQuery)
        template.execute(flushQuery)
    }

    private fun grantAdminAccess(credential: Credential) {
        val grantQuery = String.format("GRANT ALL ON %s.* TO '%s'@'%%';", dataSourceProperties.databaseName, credential.username, credential.password)
        val flushQuery = "FLUSH PRIVILEGES;"
        template.execute(grantQuery)
        template.execute(flushQuery)
    }

    private fun testCredential(credential: Credential) {
        LOG.info("Testing login for DB user {}", credential.username)
        val properties = Properties()
        properties["user"] = credential.username
        properties["password"] = credential.password
        val url = template.dataSource.connection.metaData.url
        LOG.info("Connecting to {}", url)
        DriverManager.getConnection(url, properties)
    }

}