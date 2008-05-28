package org.jetbrains.plugins.groovy.intentions.base;

import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilder;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.annotator.intentions.QuickfixUtil;
import org.jetbrains.plugins.groovy.lang.editor.template.expressions.ChooseTypeExpression;
import org.jetbrains.plugins.groovy.lang.editor.template.expressions.ParameterNameExpression;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrOpenBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrMemberOwner;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.expectedTypes.TypeConstraint;

/**
 * User: Dmitry.Krasilschikov
 * Date: 13.11.2007
 */
public class IntentionUtils {

  public static void replaceExpression(@NotNull String newExpression,
                                       @NotNull GrExpression expression)
      throws IncorrectOperationException {
    final PsiManager mgr = expression.getManager();
    final GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(expression.getProject());
    final GrExpression newCall =
        factory.createExpressionFromText(newExpression);
    final PsiElement insertedElement = expression.replaceWithExpression(newCall, true);
    //  final CodeStyleManager codeStyleManager = mgr.getCodeStyleManager();
    // codeStyleManager.reformat(insertedElement);
  }

  public static GrStatement replaceStatement(
      @NonNls @NotNull String newStatement,
      @NonNls @NotNull GrStatement statement)
      throws IncorrectOperationException {
    final PsiManager mgr = statement.getManager();
    final GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(statement.getProject());
    final GrStatement newCall =
        (GrStatement) factory.createTopElementFromText(newStatement);
    return statement.replaceWithStatement(newCall);
    //  final CodeStyleManager codeStyleManager = mgr.getCodeStyleManager();
    // codeStyleManager.reformat(insertedElement);
  }

  public static void createTemplateForMethod(PsiType[] argTypes,
                                             ChooseTypeExpression[] paramTypesExpressions,
                                             GrMethod method,
                                             GrMemberOwner owner,
                                             TypeConstraint[] constraints, boolean isConstructor) {

    Project project = owner.getProject();
    GrTypeElement typeElement = method.getReturnTypeElementGroovy();
    ParameterNameExpression paramNameExpression = new ParameterNameExpression();
    ChooseTypeExpression expr = new ChooseTypeExpression(constraints, PsiManager.getInstance(project));
    TemplateBuilder builder = new TemplateBuilder(method);
    if (!isConstructor) {
      assert typeElement != null;
      builder.replaceElement(typeElement, expr);
    }
    GrParameter[] parameters = method.getParameterList().getParameters();
    assert parameters.length == argTypes.length;
    for (int i = 0; i < parameters.length; i++) {
      GrParameter parameter = parameters[i];
      GrTypeElement parameterTypeElement = parameter.getTypeElementGroovy();
      builder.replaceElement(parameterTypeElement, paramTypesExpressions[i]);
      builder.replaceElement(parameter.getNameIdentifierGroovy(), paramNameExpression);
    }
    GrOpenBlock body = method.getBlock();
    assert body != null;
    PsiElement lbrace = body.getLBrace();
    assert lbrace != null;
    builder.setEndVariableAfter(lbrace);

    method = CodeInsightUtil.forcePsiPostprocessAndRestoreElement(method);
    Template template = builder.buildTemplate();

    Editor newEditor = QuickfixUtil.positionCursor(project, owner.getContainingFile(), method);
    TextRange range = method.getTextRange();
    newEditor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());

    TemplateManager manager = TemplateManager.getInstance(project);
    manager.startTemplate(newEditor, template);
  }
}
