/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.archetype.ui;

import org.apache.maven.archetype.ArchetypeGenerationRequest;

//TODO: We should need any remote repositories here, we should simply be doing selection, any remote catalogs
//      should be validating correctness, and if it so happens we get a crap entry then the generation mechanism

//      should take care of reporting the error. The selector should not be downloading anything.
public interface ArchetypeSelector
{
    void selectArchetype(ArchetypeGenerationRequest request, boolean interactive, String catalogs) throws Exception;
}
