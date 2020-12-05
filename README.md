## Planner based on quality models for cloud platforms

There are several mechanisms to support the adaptation decision, such as models, **rules/policies**, objectives or utility. 
This work uses **rules/policies** for the dynamic creation of adaptation plans at runtime. 

The implementation uses the framework **Drools** (Business Rules Management System), which provides a domain-specific language for defining rules, with conditions and actions, allowing to separate and reason about the logic and data collected from, for example, cloud resources.
The framework implements and extends the Rete pattern-matching algorithm. The algorithm was developed to efficiently apply many rules or patterns to many objects or facts in a knowledge base. 
It is used to determine which system rules should be triggered based on the stored data, that is, the facts.

Starting with version 5, Drools offers a new feature: **rule templates**. 
This feature allows the configuration of models that can be associated with the data. In this case, the data is separated from the rules and there are no restrictions as to which part of the rule is controlled by data. 
The use of templates has become a strong attraction for systems whose rules have an equivalent structure, but use dynamic data, such as collected and computed at runtime.

As the rules defined in the Planner infrastructure (image below, in gray) have a common structure, the implementation defines a template of Drools rules to create the rules at runtime. 
During the generation process, concrete rules regarding each attribute of the quality model are loaded from the Knowledge component and built dynamically.

![Complete quality metamodel to be used in knowledge](https://github.com/jreluiz/tma-framework-k/blob/master/database/new/models/quality_metamodel_and_planner_infrastructure.png)

The rules hierarchy is built based on the name of the rules, using the reserved word **extends**, for example:

```txt
leaf_rule_name extends composite_rule_name
```

This ensures that Drools will run the leaf_rule only if the composite_rule is satisfied.

When starting the process of building the adaptation rules, the Planner component loads the quality model stored in the Knowledge component, containing all monitored attributes, as well as the rules hierarchy with its conditions and related actions to each attribute. Then, Planner runs through the entire model (starting from the root), building the rules for each attribute.


After building the entire hierarchy of rules (with their conditions and actions) for each attribute of the quality model, the Planner component starts the compilation and execution process. Initially, it creates a new Adaptation Plan with status BUILDING, indicating that it is under construction. The execution of the rules starts from the root to the leaf attributes of the quality model. For each attribute, Planner creates a session (Knowledge Session) to operate Drools in memory. The creation of the session is done dynamically, since it uses as a parameter the rules (hierarchy built previously) associated with each attribute (attr.getRules ()) and the rules template defined:

```java
StatelessKieSession session = utility.loadSession(attr.getRules(), PropertiesManager.getInstance().getProperty("template_rules"));
```
