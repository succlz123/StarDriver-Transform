package org.succlz123.stardriver.plugin;

import org.objectweb.asm.tree.AnnotationNode;

public class ClassParameter {
    public String parentFilePath;
    // xxx.yyy.zzz
    public String className;
    public String annotationName;
    public AnnotationNode annotationNode;
    public String annotationValueName;
}
