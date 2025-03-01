/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.nfi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.nfi.backend.spi.NFIBackend;
import com.oracle.truffle.nfi.backend.spi.NFIBackendFactory;

@Bind.DefaultExpression("get($node)")
final class NFIContext {

    Env env;
    final ReentrantLock apiCacheLock = new ReentrantLock();
    final Map<String, API> apiCache = new ConcurrentHashMap<>();

    NFIContext(Env env) {
        this.env = env;
    }

    void patch(Env newEnv) {
        this.env = newEnv;
        this.apiCache.clear();
    }

    @TruffleBoundary
    API getAPI(String backendId, Node node) {
        API ret = apiCache.get(backendId);
        if (ret != null) {
            return ret;
        }

        TruffleSafepoint.setBlockedThreadInterruptible(node, ReentrantLock::lockInterruptibly, apiCacheLock);

        try {
            ret = apiCache.get(backendId);
            if (ret != null) {
                return ret;
            }

            for (LanguageInfo language : env.getInternalLanguages().values()) {
                if ("nfi".equals(language.getId())) {
                    continue;
                }

                NFIBackendFactory backendFactory = env.lookup(language, NFIBackendFactory.class);
                if (backendFactory != null && backendFactory.getBackendId().equals(backendId)) {
                    // force initialization of the backend language
                    env.initializeLanguage(language);

                    NFIBackend backend = backendFactory.createBackend(NFILanguage.get(null).nfiState);
                    if (backend != null) {
                        API api = new API(backendId, backend);
                        apiCache.put(backendFactory.getBackendId(), api);
                        return api;
                    }
                }
            }
        } finally {
            apiCacheLock.unlock();
        }

        throw new NFIParserException(String.format("Unknown NFI backend '%s'.", backendId), false);
    }

    private static final ContextReference<NFIContext> REFERENCE = ContextReference.create(NFILanguage.class);

    static NFIContext get(Node node) {
        return REFERENCE.get(node);
    }
}
