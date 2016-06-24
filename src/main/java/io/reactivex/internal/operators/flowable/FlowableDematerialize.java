/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableDematerialize<T> extends Flowable<T> {

    final Publisher<Try<Optional<T>>> source;
    
    public FlowableDematerialize(Publisher<Try<Optional<T>>> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DematerializeSubscriber<T>(s));
    }
    
    static final class DematerializeSubscriber<T> implements Subscriber<Try<Optional<T>>> {
        final Subscriber<? super T> actual;
        
        boolean done;

        Subscription s;
        
        public DematerializeSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                this.s = s;
                actual.onSubscribe(s);
            }
        }
        
        @Override
        public void onNext(Try<Optional<T>> t) {
            if (done) {
                return;
            }
            if (t.hasError()) {
                s.cancel();
                onError(t.error());
            } else {
                Optional<T> o = t.value();
                if (o.isPresent()) {
                    actual.onNext(o.get());
                } else {
                    s.cancel();
                    onComplete();
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            
            actual.onError(t);
        }
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            
            actual.onComplete();
        }
    }
}
