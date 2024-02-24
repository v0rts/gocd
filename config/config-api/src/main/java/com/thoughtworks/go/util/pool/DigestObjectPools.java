/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.util.pool;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

public class DigestObjectPools {

    public static final String SHA_256 = MessageDigestAlgorithms.SHA_256;
    public static final String MD5 = MessageDigestAlgorithms.MD5;
    public static final String SHA_512_256 = MessageDigestAlgorithms.SHA_512_256;
    private static final ThreadLocal<MessageDigest> sha256DigestLocal = new ThreadLocal<>();
    private static final ThreadLocal<MessageDigest> sha512DigestLocal = new ThreadLocal<>();
    private static final ThreadLocal<MessageDigest> md5DigestLocal = new ThreadLocal<>();
    private final CreateDigest createDigest;

    public DigestObjectPools() {
        this(new SimpleCreateDigest());
    }

    public DigestObjectPools(CreateDigest createDigest) {
        this.createDigest = createDigest;
    }

    public String computeDigest(String algorithm, DigestOperation operation) {
        if (Stream.of(SHA_512_256, SHA_256, MD5).noneMatch(s -> s.equals(algorithm))) {
            throw new IllegalArgumentException("Algorithm not supported");
        }
        try {
            MessageDigest digest = getDigest(algorithm);
            String result = operation.perform(digest);
            digest.reset(); //test passes even without this, but can't see sun impl's source, so playing safe
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute the digest.", e);
        }
    }

    private MessageDigest getDigest(String algorithm) throws Exception {
        ThreadLocal<MessageDigest> messageDigestThreadLocal = get(algorithm);
        MessageDigest digest = messageDigestThreadLocal.get();
        if (digest == null) {
            digest = createDigest.create(algorithm);
            messageDigestThreadLocal.set(digest);
        }
        return digest;
    }

    private ThreadLocal<MessageDigest> get(String algorithm) {
        return switch (algorithm) {
            case SHA_512_256 -> sha512DigestLocal;
            case SHA_256 -> sha256DigestLocal;
            case MD5 -> md5DigestLocal;
            default -> throw new IllegalArgumentException("Algorithm not supported");
        };
    }

    public interface DigestOperation {
        String perform(MessageDigest digest) throws IOException;
    }

    public interface CreateDigest {
        MessageDigest create(String algorithm) throws NoSuchAlgorithmException;
    }

    private static class SimpleCreateDigest implements CreateDigest {

        @Override
        public MessageDigest create(String algorithm) throws NoSuchAlgorithmException {
            return MessageDigest.getInstance(algorithm);
        }
    }

    @TestOnly
    void clearThreadLocals() {
        sha256DigestLocal.set(null);
        sha512DigestLocal.set(null);
        md5DigestLocal.set(null);
    }
}
