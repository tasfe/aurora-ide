package aurora.ide.meta.gef.editors.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import aurora.ide.builder.ResourceUtil;
import aurora.ide.meta.exception.ResourceNotFoundException;
import aurora.ide.meta.gef.editors.models.AuroraComponent;
import aurora.ide.meta.gef.editors.models.Grid;
import aurora.ide.meta.gef.editors.models.GridColumn;
import aurora.ide.meta.gef.editors.models.TabItem;
import aurora.ide.meta.gef.editors.models.ViewDiagram;
import aurora.ide.meta.gef.editors.property.MutilInputResourceSelector;
import aurora.ide.meta.gef.editors.template.Component;
import aurora.ide.meta.gef.editors.wizard.dialog.StyleSettingDialog;
import aurora.ide.meta.gef.i18n.Messages;
import aurora.ide.meta.project.AuroraMetaProject;

public class SetLinkOrRefWizardPage extends WizardPage {

	private Composite composite;
	private ViewDiagram viewDiagram;

	public SetLinkOrRefWizardPage() {
		super("aurora.wizard.setting.Page"); //$NON-NLS-1$
		setTitle(Messages.SettingWizardPage_Title);
		setDescription("Setting");
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		setControl(composite);
	}

	public void createCustom(ViewDiagram v, List<Grid> grids, List<TabItem> refTabItems) {
		this.viewDiagram = v;
		for (Control c : composite.getChildren()) {
			if (!c.isDisposed()) {
				c.dispose();
			}
		}

		if (refTabItems.size() > 0) {
			Group gr = new Group(composite, SWT.None);
			gr.setLayout(new GridLayout(4, false));
			gr.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			gr.setText("Set tabref");
			for (TabItem ti : refTabItems) {
				createRefField(ti, gr);
			}
		}

		if (grids.size() > 0) {
			Group gl = new Group(composite, SWT.None);
			gl.setLayout(new GridLayout(2, false));
			gl.setLayoutData(new GridData(GridData.FILL_BOTH));
			gl.setText("Set grid");
			GridData gd = new GridData(GridData.FILL_BOTH);

			TreeViewer treeViewer = new TreeViewer(gl, SWT.BORDER);
			gd.verticalSpan = 3;
			treeViewer.getTree().setLayoutData(gd);
			treeViewer.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					if(element instanceof Grid){
						return "Grid";
					}else if (element instanceof GridColumn) {
						return ((GridColumn) element).getName();
					} 
					return null;
				}

				public Image getImage(Object element) {
					return null;
				}
			});

			treeViewer.setContentProvider(new TreeContentProvider());
			treeViewer.setInput(grids);

			Button btnAdd = new Button(gl, SWT.None);
			btnAdd.setText("添加列");
			gd = new GridData();
			gd.widthHint = 80;
			gd.verticalAlignment = SWT.TOP;
			btnAdd.setLayoutData(gd);

			Button btnDel = new Button(gl, SWT.None);
			btnDel.setText("删除列");
			gd = new GridData();
			gd.widthHint = 80;
			gd.verticalAlignment = SWT.TOP;
			btnDel.setLayoutData(gd);

			Button btnModify = new Button(gl, SWT.None);
			btnModify.setText("修改列");
			gd = new GridData();
			gd.widthHint = 80;
			gd.verticalAlignment = SWT.TOP;
			btnModify.setLayoutData(gd);
		}

		composite.layout();
	}

	private void createRefField(TabItem ti, Group gr) {
		Label lbl = new Label(gr, SWT.None);
		lbl.setText("Select:");
		Text txt = new Text(gr, SWT.BORDER);
		txt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button btnSelect = new Button(gr, SWT.None);
		btnSelect.setText("选择文件");

		Button btnParam = new Button(gr, SWT.None);
		btnParam.setText("添加参数");
		btnParam.setEnabled(false);

		btnSelect.addSelectionListener(new TabRefSelect(txt, btnParam, ti));

		btnParam.addSelectionListener(new TabRefParamSelect(ti));
	}

	private IProject getMetaProject() {
		for (IWizardPage page = this; page.getPreviousPage() != null; page = page.getPreviousPage()) {
			if (page instanceof NewWizardPage) {
				return ((NewWizardPage) page).getMetaProject();
			}
		}
		return null;
	}

	private IProject getAuroraProject() {
		try {
			return new AuroraMetaProject(getMetaProject()).getAuroraProject();
		} catch (ResourceNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Object fileSelect(IContainer[] containers, String[] extFilter) {
		MutilInputResourceSelector fss = new MutilInputResourceSelector(getShell());
		fss.setExtFilter(extFilter);
		fss.setInputs(containers);
		Object obj = fss.getSelection();
		return obj;
	}

	public ViewDiagram getViewDiagram() {
		return viewDiagram;
	}

	public void setViewDiagram(ViewDiagram viewDiagram) {
		this.viewDiagram = viewDiagram;
	}

	class TabRefSelect extends SelectionAdapter {
		private Text txt;
		private TabItem ti;
		private Button btn;

		public TabRefSelect(Text txt, Button btn, TabItem ti) {
			this.txt = txt;
			this.ti = ti;
			this.btn = btn;
		}

		public void widgetSelected(SelectionEvent e) {
			String webHome = ResourceUtil.getWebHome(getAuroraProject());
			IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(webHome);
			IContainer uipFolder = getMetaProject().getFolder("ui_prototype");
			Object obj = fileSelect(new IContainer[] { (IContainer) res, uipFolder }, new String[] { "screen", "uip" });
			if (!(obj instanceof IFile)) {
				txt.setText("");
				btn.setEnabled(false);
			} else {
				String path = ((IFile) obj).getFullPath().toString();
				txt.setText(path);
				if (path.endsWith("uip")) {
					path = path.substring(path.indexOf("ui_prototype") + "ui_prototype".length());
				} else if (path.endsWith("screen")) {
					path = path.substring(path.indexOf(webHome) + webHome.length());
				}
				ti.getTabRef().setOpenPath(path);
				btn.setEnabled(true);
			}
		}
	}

	class TabRefParamSelect extends SelectionAdapter {
		private TabItem ti;

		public TabRefParamSelect(TabItem ti) {
			this.ti = ti;
		}

		public void widgetSelected(SelectionEvent e) {
			StyleSettingDialog dialog = new StyleSettingDialog(getShell(), ti.getTabRef().getParameters());
			if (dialog.open() == Dialog.OK) {
				ti.getTabRef().addAllParameter(dialog.getResult());
			}
		}
	}

	class TreeContentProvider implements ITreeContentProvider {
		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List) {
				return ((List<?>) inputElement).toArray();
			}
			return null;
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Grid) {
				Grid grid = (Grid) parentElement;
				List<Object> gc = new ArrayList<Object>();
				for (Object obj : grid.getChildren()) {
					if (obj instanceof GridColumn) {
						gc.add(obj);
					}
				}
				return gc.toArray();
			}
			return null;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof Grid) {
				return true;
			}
			return false;
		}

	}
}
