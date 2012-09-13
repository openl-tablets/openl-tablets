package org.openl.rules.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.validation.RulesDeployValidator;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

public class RulesDeployManager {
    private IRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
    private RulesDeployValidator validator = new RulesDeployValidator();
    private PathMatcher pathMatcher = new AntPathMatcher();

    public PathMatcher getPathMatcher() {
        return pathMatcher;
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    public IRulesDeploySerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(IRulesDeploySerializer serializer) {
        this.serializer = serializer;
    }

    private RulesDeploy readDescriptorInternal(InputStream source) {
        return serializer.deserialize(source);
    }

    public RulesDeploy readDescriptor(File filename) throws FileNotFoundException, ValidationException {
        FileInputStream inputStream = new FileInputStream(filename);

        RulesDeploy rulesDeploy = readDescriptorInternal(inputStream);
        postProcess(rulesDeploy, filename);
        validator.validate(rulesDeploy);

        return rulesDeploy;
    }
    
    public RulesDeploy readDescriptor(String filename) throws FileNotFoundException, ValidationException {
        File source = new File(filename);
        return readDescriptor(source);
    }

    public void writeDescriptor(RulesDeploy rulesDeploy, OutputStream dest) throws IOException,
                                                                                ValidationException {
        validator.validate(rulesDeploy);

        String serializedObject = serializer.serialize(rulesDeploy);
        dest.write(serializedObject.getBytes());
    }
    
    private void postProcess(RulesDeploy rulesDeploy, File rulesDeployFile) {
        
    }

}
