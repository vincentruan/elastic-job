/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package com.dangdang.ddframe.job.internal.job;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import lombok.extern.slf4j.Slf4j;

import com.dangdang.ddframe.job.api.DataFlowElasticJob;
import com.dangdang.ddframe.job.internal.statistics.ProcessCountStatistics;

/**
 * 用于处理数据流程的作业抽象类.
 * 
 * @author zhangliang
 * 
 * @param <T> 数据流作业处理的数据实体类型
 * 
 * @param <C> 作业运行时分片上下文类型
 */
@Slf4j
public abstract class AbstractDataFlowElasticJob<T, C extends AbstractJobExecutionShardingContext> extends AbstractElasticJob implements DataFlowElasticJob<T, C> {
    
    @Override
    public final void updateOffset(final int item, final String offset) {
        getOffsetService().updateOffset(item, offset);
    }

    /**
     * 处理分片数据
     * @param shardingContext 作业分片规则配置上下文
     * @param data 待处理的数据
     */
    protected final void processDataWithStatistics(final C shardingContext, final List<T> data) {
        for (T each : data) {
            processDataWithStatistics(shardingContext, each);
        }
    }

    /**
     * 处理分片数据
     * @param shardingContext 作业分片规则配置上下文
     * @param data 待处理的数据
     */
    protected final void processDataWithStatistics(final C shardingContext, final T data) {
        try {
            // CHECKSTYLE:OFF
            if (processData(shardingContext, data)) {
                ProcessCountStatistics.incrementProcessSuccessCount(shardingContext.getJobName());
            } else {
                ProcessCountStatistics.incrementProcessFailureCount(shardingContext.getJobName());
            }
        } catch (final Exception ex) {
            ProcessCountStatistics.incrementProcessFailureCount(shardingContext.getJobName());
            // CHECKSTYLE:ON
            log.error("Elastic job: exception occur in job processing...", ex);
        }
    }
    
    protected final void latchAwait(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
