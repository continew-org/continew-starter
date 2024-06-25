/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.continew.starter.core.autoconfigure.threadpool;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池拒绝策略
 *
 * @author Charles7c
 * @since 2.2.0
 */
public enum ThreadPoolExecutorRejectedPolicy {

    /**
     * ThreadPoolTaskExecutor 默认的拒绝策略，不执行新任务，直接抛出 RejectedExecutionException 异常
     */
    ABORT {
        @Override
        public RejectedExecutionHandler getRejectedExecutionHandler() {
            return new ThreadPoolExecutor.AbortPolicy();
        }
    },

    /**
     * 提交的任务在执行被拒绝时，会由提交任务的线程去执行
     */
    CALLER_RUNS {
        @Override
        public RejectedExecutionHandler getRejectedExecutionHandler() {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        }
    },

    /**
     * 不执行新任务，也不抛出异常
     */
    DISCARD {
        @Override
        public RejectedExecutionHandler getRejectedExecutionHandler() {
            return new ThreadPoolExecutor.DiscardPolicy();
        }
    },

    /**
     * 拒绝新任务，但是会抛弃队列中最老的任务，然后尝试再次提交新任务
     */
    DISCARD_OLDEST {
        @Override
        public RejectedExecutionHandler getRejectedExecutionHandler() {
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        }
    };

    /**
     * 获取拒绝处理器
     *
     * @return 拒绝处理器
     */
    public abstract RejectedExecutionHandler getRejectedExecutionHandler();
}
