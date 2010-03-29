package org.openl.meta;

import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

/**
 *
 * @author snshor
 */

/**
 *
 * The interface IVocabulary has a special meaning. The instances of the
 * interface is going to be used by multiple OpenL Tablets tools.
 * <p>
 * It's main function is to provide a set of <i>instrumented</i> types that
 * later can be used in all OpenL components.
 * <p>
 * Even though generic OpenL provides a wider possibilities to define types than
 * conventional Java, the most of types used in OpenL are POJOs. As such they
 * contain minimum of meta-information. The use of Java annotations is limited
 * to Java programmers. The intention of IVocabulary interface is to provide
 * means for business users to define meta-information for Business Objects. At
 * the same time it provides enough freedom for developers to define custom
 * types and plug them in.
 * <p>
 * The instrumentation enhances OpenL Types (usually derived from POJO) can be
 * done through using a separate OpenL Tablets module. The "vocabulary" module
 * can be shared between multiple OpenL projects.
 *
 * <p>
 * <h2>Configuration</h2>
 * <p>
 * IVocabulary implementation must be a public class with public default
 * constructor located in project's classpath. In case, when the implementation
 * is just plain Java, this will suffice. In more probable case the class will
 * be based on OpenL Tablets module(the domains will be defined in .xls file by
 * business users using either Data or Decision Tables). In this case the
 * special care should be taken about proper location of .xls file, because the
 * module can be defined in a different Eclipse project. By default, the files
 * are located in <code>rules/</code> folder. This works fine, if the
 * vocabulary module is located in the same project as the main project. In case
 * if the vocabulary project is separate(often the case when vocabulary is
 * shared) the location should be defined using that project's name:
 * <code>../my.vocabularyproject/rules/MyVocabulary.xls</code>. The change
 * needs to be made in <code>build/GenerateJavaWrapper.build.xml</code> file.
 *
 *
 */

public interface IVocabulary {

    /**
     *
     * @return an array of Vocabulary Types that will be available as types in
     *         all OpenL components.
     * @throws BoundError
     *
     */
    IOpenClass[] getVocabularyTypes() throws SyntaxNodeException;

}
