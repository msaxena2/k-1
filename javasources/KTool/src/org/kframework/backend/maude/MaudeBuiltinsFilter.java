package org.kframework.backend.maude;

import org.kframework.backend.BackendFilter;
import org.kframework.kil.Attribute;
import org.kframework.kil.Configuration;
import org.kframework.kil.KSorts;
import org.kframework.kil.Production;
import org.kframework.kil.Rule;
import org.kframework.kil.Sort;
import org.kframework.kil.Variable;
import org.kframework.kil.loader.Context;
import org.kframework.utils.StringUtil;

import java.util.Properties;


/**
 * Visitor generating the maude equations hooking the builtins from the hooked productions.
 */
public class MaudeBuiltinsFilter extends BackendFilter {
	private String left, right;
	private boolean first;
	private final Properties builtinsProperties;

	public MaudeBuiltinsFilter(Properties builtinsProperties, Context context) {
		super(context);
		this.builtinsProperties = builtinsProperties;
	}

	@Override
	public void visit(Configuration node) {
		return;
	}

	@Override
	public void visit(org.kframework.kil.Context node) {
		return;
	}

	@Override
	public void visit(Rule node) {
		return;
	}

	@Override
	public void visit(Production node) {
        if (!node.containsAttribute(Attribute.HOOK_KEY)) {
			return;
		}

        final String hook = node.getAttribute(Attribute.HOOK_KEY);
		if (builtinsProperties.containsKey(hook)) {
			result.append(builtinsProperties.getProperty(hook));
			result.append("\n");
			return;
		}

        result.append(" eq ");
		left = StringUtil.escapeMaude(node.getKLabel());
        left += "(";
		right = getHookLabel(hook);
        if (!node.isConstant()) {
            right += "(";
            first = true;
            super.visit(node);
            right += ")";
        } else {
            left += ".KList";
        }
        left += ")";
		result.append(left);
		result.append(" = _`(_`)(");
        if (context.collectionSorts.containsKey(node.getSort())) {
            result.append(context.collectionSorts.get(node.getSort()).type() + "2KLabel_(");
        } else {
            result.append("#_(");
        }
		result.append(right);
		result.append("), .KList)");
        result.append(" .\n");
	}


	@Override
	public void visit(Sort node) {
		if (!first) {
			left += ",, ";
			right += ", ";
		} else {
			first = false;
		}

        Variable var;
        if (context.collectionSorts.containsKey(node.getName())
                || node.getName().equals(KSorts.K)
                || node.getName().equals(KSorts.KITEM)) {
            var = Variable.getFreshVar(node.getName());
        } else {
            var = Variable.getFreshVar("#" + node.getName());
        }

        MaudeFilter filter = new MaudeFilter(context);
		filter.visit(var);
        left += filter.getResult();

        if (context.collectionSorts.containsKey(node.getName())) {
            var.setSort(context.collectionSorts.get(node.getName()).type());
        }
		right += var.toString();
	}

	private String getHookLabel(String hook) {
		return hook.split(":")[1];
	}

}
