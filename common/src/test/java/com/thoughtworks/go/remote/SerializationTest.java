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

package com.thoughtworks.go.remote;

import com.google.gson.Gson;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.helper.ReversingEncrypter;
import com.thoughtworks.go.security.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.apache.commons.text.StringEscapeUtils.escapeJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings("deprecation")
class SerializationTest {
    @Test
    void rejectsSerializationOfGoCipher() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().toJson(new GoCipher(mock(Encrypter.class))));
        assertEquals(format("Refusing to serialize a %s instance and leak security details!", GoCipher.class.getName()), e.getMessage());
    }

    @Test
    void rejectsDeserializationOfGoCipher() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{}", GoCipher.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", GoCipher.class.getName()), e.getMessage());
    }

    @Test
    void rejectsSerializationOfAESEncrypter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().toJson(new AESEncrypter(mock(AESCipherProvider.class))));
        assertEquals(format("Refusing to serialize a %s instance and leak security details!", AESEncrypter.class.getName()), e.getMessage());
    }

    @Test
    void rejectsDeserializationOfAESEncrypter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{}", AESEncrypter.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", AESEncrypter.class.getName()), e.getMessage());
    }

    @Test
    void rejectsSerializationOfAESCipherProvider() {
        final AESCipherProvider acp = new AESCipherProvider(new TempSystemEnvironment());
        try {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                    Serialization.instance().toJson(acp));
            assertEquals(format("Refusing to serialize a %s instance and leak security details!", AESCipherProvider.class.getName()), e.getMessage());
        } finally {
            acp.removeCachedKey();
        }
    }

    @Test
    void rejectsDeserializationOfAESCipherProvider() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{}", AESCipherProvider.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", AESCipherProvider.class.getName()), e.getMessage());
    }

    @Test
    void rejectsSerializationOfDESEncrypter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().toJson(new DESEncrypter(mock(DESCipherProvider.class))));
        assertEquals(format("Refusing to serialize a %s instance and leak security details!", DESEncrypter.class.getName()), e.getMessage());
    }

    @Test
    void rejectsDeserializationOfDESEncrypter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{}", DESEncrypter.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", DESEncrypter.class.getName()), e.getMessage());
    }

    @Test
    void rejectsSerializationOfDESCipherProvider() {
        final DESCipherProvider dcp = new DESCipherProvider(new TempSystemEnvironment());
        try {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                    Serialization.instance().toJson(dcp));
            assertEquals(format("Refusing to serialize a %s instance and leak security details!", DESCipherProvider.class.getName()), e.getMessage());
        } finally {
            dcp.removeCachedKey();
        }
    }

    @Test
    void rejectsDeserializationOfDESCipherProvider() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                Serialization.instance().fromJson("{ \"whatever\": \"actual payload doesn't matter\" }", DESCipherProvider.class));
        assertEquals(format("Refusing to deserialize a %s in the JSON stream!", DESCipherProvider.class.getName()), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/linux/style", "windows\\style"})
    void deserializesFilesNormalizingPathToPlatform(String rawPath) {
        String json = format("{ \"path\": \"%s\" }", escapeJson(rawPath));
        assertThat(Serialization.instance().fromJson(json, File.class))
                .isEqualTo(new File(separatorsToSystem(rawPath)));
    }

    @Test
    void serializesFiles() {
        File file = new File("/hello/world");
        assertThatJson(Serialization.instance().toJson(file))
                .isEqualTo(format("{\"path\":\"%s\"}", escapeJson(separatorsToSystem(file.getPath()))));
    }

    @Test
    void serializesCharsets() {
        assertThatJson(Serialization.instance().toJson(StandardCharsets.UTF_8)).isEqualTo("UTF-8");
        assertThatJson(Serialization.instance().toJson(Charset.forName("ascii"))).isEqualTo("US-ASCII");
    }

    @Test
    void deserializesCharsets() {
        assertEquals(StandardCharsets.UTF_8, Serialization.instance().fromJson("UTF-8", Charset.class));
        assertEquals(StandardCharsets.US_ASCII, Serialization.instance().fromJson("ascii", Charset.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void successfullySerializesConfigurationPropertyBecauseGoCipherIsHiddenFromSerialization() {
        assertDoesNotThrow(() -> {
            final String json = Serialization.instance().toJson(new ConfigurationProperty(dumbcipher())
                    .withKey("hello")
                    .withEncryptedValue("dlrow"));
            Map<String, String> actual = new Gson().fromJson(json, Map.class);
            assertEquals(2, actual.size());
            assertEquals("hello", actual.get("key"));
            assertEquals("world", actual.get("value"));
        }, "ConfigurationProperty should serialize without error because its type adapter hides the nested GoCipher from Gson");
    }

    @Test
    void successfullyDeserializesConfigurationPropertyBecauseGoCipherIsNeverUsed() {
        assertDoesNotThrow(() -> {
            final ConfigurationProperty store = Serialization.instance().fromJson(
                    "{\"key\": \"one\", \"value\": \"a\"}", ConfigurationProperty.class);
            assertEquals("one", store.getConfigKeyName());
            assertEquals("a", store.getValue());
        }, "ConfigurationProperty should deserialize without error because its type adapter never deserializes as a secret property");
    }

    @SuppressWarnings("unchecked")
    @Test
    void successfullySerializesArtifactStoreBecauseGoCipherIsHiddenFromSerialization() {
        assertDoesNotThrow(() -> {
            final String json = Serialization.instance().toJson(
                    new ArtifactStore("store", "plugin",
                            plainProperty("plain", "text"),
                            secretProperty("secret", "!llet t'nod"))
            );
            Map<String, Object> actual = new Gson().fromJson(json, Map.class);
            List<Map<String, Object>> props = (List<Map<String, Object>>) actual.get("configuration");
            assertEquals(2, props.size());
            assertEquals("plain", props.get(0).get("key"));
            assertEquals("text", props.get(0).get("value"));
            assertEquals("secret", props.get(1).get("key"));
            assertEquals("don't tell!", props.get(1).get("value"));
        }, "ArtifactStore should serialize without error because its type adapter hides the nested GoCipher from Gson");
    }

    @Test
    void successfullyDeserializesArtifactStoreBecauseGoCipherIsNeverUsed() {
        assertDoesNotThrow(() -> {
            final ArtifactStore store = Serialization.instance().fromJson("{" +
                    "\"id\": \"store\"," +
                    "\"pluginId\": \"plugin\"," +
                    "\"configuration\": [" +
                    "{\"key\": \"one\", \"value\": \"a\"}," +
                    "{\"key\": \"two\", \"value\": \"b\"}" +
                    "]" +
                    "}", ArtifactStore.class);
            assertEquals("store", store.getId());
            assertEquals("plugin", store.getPluginId());

            final Map<String, String> config = store.getConfigurationAsMap(true, true);
            assertEquals(2, config.size());
            assertEquals("a", config.get("one"));
            assertEquals("b", config.get("two"));
        }, "ArtifactStore should deserialize without error because its type adapter never deserializes to secret properties");
    }

    @Test
    void configurationPropertyWithSecretParamsShouldSerializeResolvedValues() {
        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("db_password"),
                new ConfigurationValue("{{SECRET:[test_id][password]}}"));
        configurationProperty.getSecretParams().get(0).setValue("secret");

        String json = Serialization.instance().toJson(configurationProperty);

        assertThat(json).isEqualTo("{\"key\":\"db_password\",\"value\":\"secret\"}");
    }

    @SuppressWarnings("SameParameterValue")
    private ConfigurationProperty plainProperty(String name, String value) {
        return new ConfigurationProperty(new ConfigurationKey(name), new ConfigurationValue(value), null, dumbcipher());
    }

    private GoCipher dumbcipher() {
        return new GoCipher(new ReversingEncrypter());
    }

    @SuppressWarnings("SameParameterValue")
    private ConfigurationProperty secretProperty(String name, String encryptedValue) {
        return new ConfigurationProperty(new ConfigurationKey(name), null, new EncryptedConfigurationValue(encryptedValue), dumbcipher());
    }

    private static class TempSystemEnvironment extends SystemEnvironment {
        @Override
        public File getAESCipherFile() {
            final File file;
            try {
                file = File.createTempFile("aes", null);
                file.deleteOnExit();
            } catch (IOException e) {
                throw new RuntimeException("failed to create tempfile for testing AES cipher file");
            }
            return file;
        }

        @Override
        public File getDESCipherFile() {
            final File file;
            try {
                file = File.createTempFile("des", null);
                file.deleteOnExit();
            } catch (IOException e) {
                throw new RuntimeException("failed to create tempfile for testing DES cipher file");
            }
            return file;
        }
    }

}
