---
title: "Sample"
---
<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# Sample
<table align="left">
    <a target="_blank" class="button"
        href="https://beam.apache.org/releases/javadoc/current/org/apache/beam/sdk/transforms/Sample.html">
      <img src="/images/logos/sdks/java.png" width="20px" height="20px"
           alt="Javadoc" />
     Javadoc
    </a>
</table>
<br><br>

Transforms for taking samples of the elements in a collection, or
samples of the values associated with each key in a collection of key-value pairs.

## Examples

{{< playground height="700px" >}}
{{< playground_snippet language="java" path="SDK_JAVA_Sample" show="main_section" >}}
{{< /playground >}}

## Related transforms
* [Top](/documentation/transforms/java/aggregation/top)
  finds the largest (or smallest) set of elements in a collection
* [Latest](/documentation/transforms/java/aggregation/latest)
  computes the latest element in a collection
