/*
 * Copyright 2025 Google LLC
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

package com.google.adk.tools.mcp;

import com.google.adk.agents.ReadonlyContext;
import com.google.adk.tools.BaseTool;
import com.google.common.collect.ImmutableList;
import static com.google.common.truth.Truth.assertThat;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import static org.junit.Assert.assertThrows;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import reactor.core.publisher.Mono;

import java.util.List;

/** Unit tests for {@link McpAsyncToolset}. */
@RunWith(JUnit4.class)
public final class McpAsyncToolsetTest {

  @Rule public final MockitoRule mocks = MockitoJUnit.rule();

  @Mock private McpSessionManager mockMcpSessionManager;
  @Mock private McpAsyncClient mockMcpAsyncClient;
  @Mock private ReadonlyContext mockReadonlyContext;

  private static final McpJsonMapper jsonMapper = McpJsonMapper.getDefault();

  @Test
  public void builder_withBothConnectionParamsAndMcpSessionManager_throwsIllegalState() {
    McpAsyncToolset.Builder builder =
        new McpAsyncToolset.Builder()
            .connectionParams(SseServerParameters.builder().url("http://localhost:8080").build())
            .mcpSessionManager(mockMcpSessionManager);

    IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

    assertThat(exception)
        .hasMessageThat()
        .contains("Only one of connectionParams or mcpSessionManager may be set, not both.");
  }

  @Test
  public void builder_withNeitherConnectionParamsNorMcpSessionManager_throwsIllegalState() {
    McpAsyncToolset.Builder builder = new McpAsyncToolset.Builder();

    IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

    assertThat(exception)
        .hasMessageThat()
        .contains("One of connectionParams or mcpSessionManager must be set.");
  }

  @Test
  public void builder_withMcpSessionManager_buildsSuccessfully() {
    McpAsyncToolset toolset =
        new McpAsyncToolset.Builder().mcpSessionManager(mockMcpSessionManager).build();

    assertThat(toolset).isNotNull();
  }

  @Test
  public void builder_withSseConnectionParams_buildsSuccessfully() {
    McpAsyncToolset toolset =
        new McpAsyncToolset.Builder()
            .connectionParams(SseServerParameters.builder().url("http://localhost:8080").build())
            .build();

    assertThat(toolset).isNotNull();
  }

  @Test
  public void getTools_withCustomMcpSessionManager_returnsTools() {
    McpSchema.Tool mockTool =
        McpSchema.Tool.builder()
            .name("my_tool")
            .description("A tool")
            .inputSchema(jsonMapper, "{}")
            .build();
    McpSchema.ListToolsResult mockResult =
        new McpSchema.ListToolsResult(ImmutableList.of(mockTool), null);

    when(mockMcpSessionManager.createAsyncSession()).thenReturn(mockMcpAsyncClient);
    when(mockMcpAsyncClient.initialize())
        .thenReturn(
            Mono.just(
                new McpSchema.InitializeResult(
                    "2024-11-05",
                    new McpSchema.ServerCapabilities(null, null, null, null, null, null),
                    new McpSchema.Implementation("test-server", "1.0"),
                    null)));
    when(mockMcpAsyncClient.listTools()).thenReturn(Mono.just(mockResult));

    McpAsyncToolset toolset =
        new McpAsyncToolset.Builder().mcpSessionManager(mockMcpSessionManager).build();

    List<BaseTool> tools = toolset.getTools(mockReadonlyContext).toList().blockingGet();

    assertThat(tools.stream().map(BaseTool::name).collect(ImmutableList.toImmutableList()))
        .containsExactly("my_tool");
    verify(mockMcpSessionManager).createAsyncSession();
    verify(mockMcpAsyncClient).initialize();
    verify(mockMcpAsyncClient).listTools();
  }

  @Test
  public void getTools_withCustomMcpSessionManagerAndToolFilter_returnsFilteredTools() {
    McpSchema.Tool mockTool1 =
        McpSchema.Tool.builder()
            .name("tool1")
            .description("desc1")
            .inputSchema(jsonMapper, "{}")
            .build();
    McpSchema.Tool mockTool2 =
        McpSchema.Tool.builder()
            .name("tool2")
            .description("desc2")
            .inputSchema(jsonMapper, "{}")
            .build();
    McpSchema.Tool mockTool3 =
        McpSchema.Tool.builder()
            .name("tool3")
            .description("desc3")
            .inputSchema(jsonMapper, "{}")
            .build();
    McpSchema.ListToolsResult mockResult =
        new McpSchema.ListToolsResult(ImmutableList.of(mockTool1, mockTool2, mockTool3), null);

    when(mockMcpSessionManager.createAsyncSession()).thenReturn(mockMcpAsyncClient);
    when(mockMcpAsyncClient.initialize())
        .thenReturn(
            Mono.just(
                new McpSchema.InitializeResult(
                    "2024-11-05",
                    new McpSchema.ServerCapabilities(null, null, null, null, null, null),
                    new McpSchema.Implementation("test-server", "1.0"),
                    null)));
    when(mockMcpAsyncClient.listTools()).thenReturn(Mono.just(mockResult));

    McpAsyncToolset toolset =
        new McpAsyncToolset.Builder()
            .mcpSessionManager(mockMcpSessionManager)
            .toolFilter(ImmutableList.of("tool1", "tool3"))
            .build();

    List<BaseTool> tools = toolset.getTools(mockReadonlyContext).toList().blockingGet();

    assertThat(tools.stream().map(BaseTool::name).collect(ImmutableList.toImmutableList()))
        .containsExactly("tool1", "tool3")
        .inOrder();
  }
}
