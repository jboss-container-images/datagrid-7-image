package org.infinispan.configuration.converter.assertions;

import org.assertj.core.api.AbstractAssert;
import org.infinispan.configuration.converter.util.ConfigurationScriptInvoker;

public class ResultAssertion extends AbstractAssert<ResultAssertion, ConfigurationScriptInvoker.Result> {

   private ResultAssertion(ConfigurationScriptInvoker.Result result) {
      super(result, ResultAssertion.class);
   }

   public static ResultAssertion assertThat(ConfigurationScriptInvoker.Result result) {
      return new ResultAssertion(result);
   }

   public ResultAssertion isOk() {
      if (actual.getResultCode() != 0) {
         failWithMessage("The script returned with error code %s. Script's output:\n%s", actual.getResultCode(), actual.getOutput());
      }
      return this;
   }

   public ResultAssertion printResult() {
      System.out.println(actual.getOutput());
      return this;
   }


}
