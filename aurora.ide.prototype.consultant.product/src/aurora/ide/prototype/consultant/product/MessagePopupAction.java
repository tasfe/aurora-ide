package aurora.ide.prototype.consultant.product;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;


public class MessagePopupAction extends Action {

    private final IWorkbenchWindow window;

    MessagePopupAction(String text, IWorkbenchWindow window) {
        super(text);
        this.window = window;
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_OPEN_MESSAGE);
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_OPEN_MESSAGE);
        setImageDescriptor(aurora.ide.prototype.consultant.product.Activator.getImageDescriptor("/icons/sample3.gif"));
    }

    public void run() {
    	FileDialog sd = new FileDialog(window.getShell(),SWT.SAVE);
    	
    	sd.open();
//        MessageDialog.openInformation(window.getShell(), "Open", "Open Message Dialog!");
    }
}