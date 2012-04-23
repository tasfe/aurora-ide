package aurora.ide.meta.gef.editors.wizard;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import aurora.ide.api.composite.map.CommentCompositeMap;
import aurora.ide.helpers.DialogUtil;
import aurora.ide.meta.gef.editors.VScreenEditor;
import aurora.ide.meta.gef.editors.models.Grid;
import aurora.ide.meta.gef.editors.models.TabItem;
import aurora.ide.meta.gef.editors.models.ViewDiagram;
import aurora.ide.meta.gef.editors.models.io.ModelIOManager;
import aurora.ide.meta.gef.editors.template.Template;
import aurora.ide.search.ui.EditorOpener;

public class CreateMetaWizard extends Wizard implements INewWizard {
	private NewWizardPage newPage = new NewWizardPage();
	private SelectModelWizardPage selectPage = new SelectModelWizardPage();
	private SetLinkOrRefWizardPage settingPage = new SetLinkOrRefWizardPage();

	private IWorkbench workbench;
	private ViewDiagram viewDiagram;

	// private Template template;
	// private Map<String, IFile> modelMap = new HashMap<String, IFile>();
	// private Map<String, AuroraComponent> acptMap = new HashMap<String,
	// AuroraComponent>();
	// private Map<String, String> queryMap = new HashMap<String, String>();
	// private List<InitModel> initModels = new ArrayList<InitModel>();
	// private int tabItemIndex = 0;

	private Template template;

	public void addPages() {
		addPage(newPage);
		addPage(selectPage);
		addPage(settingPage);
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		WizardDialog dialog = (WizardDialog) getContainer();
		dialog.addPageChangingListener(new IPageChangingListener() {
			public void handlePageChanging(PageChangingEvent event) {
				if (event.getCurrentPage() == newPage && event.getTargetPage() == selectPage) {
					IProject metaProject = newPage.getMetaProject();
					if (metaProject != null && template != newPage.getTemplate()) {
						template = newPage.getTemplate();
						selectPage.setBMPath(metaProject);
						selectPage.createDynamicTextComponents(template);
					}
				} else if (event.getCurrentPage() == selectPage && event.getTargetPage() == settingPage) {
					if (viewDiagram != selectPage.getViewDiagram()) {
						List<Grid> grids = selectPage.getGrids();
						List<TabItem> refTabItems = selectPage.getRefTabItems();
						viewDiagram = selectPage.getViewDiagram();
						settingPage.createCustom(viewDiagram, grids, refTabItems);
					}
				}
			}
		});
	}

	@Override
	public boolean performFinish() {
		if (viewDiagram == null) {
			viewDiagram = selectPage.getViewDiagram();
		}
		EditorOpener editorOpener = new EditorOpener();
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(newPage.getPath() + "/" + newPage.getFileName()));
		CommentCompositeMap rootMap = null;
		try {
			rootMap = (CommentCompositeMap) ModelIOManager.getNewInstance().toCompositeMap(viewDiagram);
		} catch (RuntimeException e) {
			e.printStackTrace();
			DialogUtil.showExceptionMessageBox(e);
			return false;
		}
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + rootMap.toXML();
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		final CreateFileOperation cfo = new CreateFileOperation(file, null, is, "create template.");
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					cfo.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		};
		try {
			getContainer().run(true, true, op);
			IEditorPart editor = editorOpener.open(workbench.getActiveWorkbenchWindow().getActivePage(), file, true);
			if (editor instanceof VScreenEditor) {
				((VScreenEditor) editor).markDirty();
			}
			return true;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return false;
	}

	// private ViewDiagram createView() {
	// ViewDiagram viewDiagram = new ViewDiagram();
	// tabItemIndex = 0;
	// for (Component cpt : template.getChildren()) {
	// AuroraComponent ac = createAuroraComponent(cpt);
	// if (ac instanceof AuroraComponent) {
	// viewDiagram.addChild(ac);
	// }
	// }
	// fillQueryField();
	// fillButtonTarget();
	// viewDiagram.setTemplateType(template.getType());
	// viewDiagram.setBindTemplate(template.getPath());
	// for (InitModel im : initModels) {
	// viewDiagram.getInitModels().add(im);
	// }
	// return viewDiagram;
	// }
	//
	// private void fillButtonTarget() {
	// for (String s : queryMap.keySet()) {
	// Object obj = acptMap.get(s);
	// if (obj instanceof Button) {
	// Button btn = (Button) obj;
	// AuroraComponent ac = acptMap.get(queryMap.get(s));
	// btn.getButtonClicker().setTargetComponent(ac);
	// } else if (obj instanceof Grid) {
	// Grid bc = (Grid) obj;
	// AuroraComponent ac = acptMap.get(queryMap.get(s));
	// if (ac instanceof Container) {
	// bc.getDataset().setQueryContainer((Container) ac);
	// }
	// }
	// }
	// }
	//
	// private void fillQueryField() {
	// for (String mid : modelMap.keySet()) {
	// Object obj = acptMap.get(mid);
	// if (!(obj instanceof Container)) {
	// continue;
	// }
	// if (((Container) obj).getSectionType() == null || "".equals(((Container)
	// obj).getSectionType())) {
	// ((Container) obj).setSectionType(Container.SECTION_TYPE_QUERY);
	// String s = getBmPath(modelMap.get(mid));
	// QueryDataSet ds = (QueryDataSet) ((Container) obj).getDataset();
	// ds.setModel(s);
	// ((Container) obj).setDataset(ds);
	// }
	// if (Template.TYPE_DISPLAY.equals(template.getType())) {
	// CommentCompositeMap map = GefModelAssist.getModel(modelMap.get(mid));
	// for (CommentCompositeMap queryMap :
	// GefModelAssist.getQueryFields(GefModelAssist.getModel(modelMap.get(mid))))
	// {
	// Label label = new Label();
	// ((Container) obj).addChild(createField(label, map, queryMap));
	// }
	// } else {
	// CommentCompositeMap map = GefModelAssist.getModel(modelMap.get(mid));
	// for (CommentCompositeMap queryMap :
	// GefModelAssist.getQueryFields(GefModelAssist.getModel(modelMap.get(mid))))
	// {
	// Input input = new Input();
	// ((Container) obj).addChild(createField(input, map, queryMap));
	// }
	// }
	// }
	// }
	//
	// private AuroraComponent createField(AuroraComponent ac,
	// CommentCompositeMap map, CommentCompositeMap queryMap) {
	// CommentCompositeMap fieldMap =
	// GefModelAssist.getCompositeMap((CommentCompositeMap)
	// map.getChild("fields"), "name", queryMap.getString("field"));
	// if (fieldMap == null) {
	// fieldMap = queryMap;
	// }
	// ac.setName(fieldMap.getString("name"));
	// ac.setPrompt(fieldMap.getString("prompt") == null ?
	// fieldMap.getString("name") : fieldMap.getString("prompt"));
	// ac.setType(GefModelAssist.getTypeNotNull(fieldMap));
	// return ac;
	// }
	//
	// private AuroraComponent createAuroraComponent(Component cpt) {
	// AuroraComponent acpt =
	// AuroraModelFactory.createComponent(cpt.getComponentType());
	// if (acpt == null) {
	// return null;
	// }
	// if (acpt instanceof TabItem) {
	// ((TabItem) acpt).setPrompt("tabItem" + tabItemIndex++);
	// }
	// if (null != cpt.getId() && !"".equals(cpt.getId())) {
	// acptMap.put(cpt.getId(), acpt);
	// }
	// if ((cpt instanceof BMBindComponent) && (acpt instanceof Container)) {
	// fillContainer(cpt, (Container) acpt);
	// }
	// if (cpt.getChildren() == null) {
	// return acpt;
	// }
	// for (Component cp : cpt.getChildren()) {
	// if (fillTabRef(acpt, cp)) {
	// continue;
	// }
	// AuroraComponent ac = createAuroraComponent(cp);
	// fillButton(cp, ac);
	// if (acpt instanceof Container) {
	// ((Container) acpt).addChild(ac);
	// }
	// }
	// return acpt;
	// }
	//
	// private void fillButton(Component cp, AuroraComponent ac) {
	// if (ac instanceof Button) {
	// aurora.ide.meta.gef.editors.template.Button btn =
	// (aurora.ide.meta.gef.editors.template.Button) cp;
	// ((Button) ac).setText(btn.getText());
	// if (("toolBar".equals(btn.getParent().getComponentType())) &&
	// btn.getType() != null && contains(Button.std_types, btn.getType())) {
	// ((Button) ac).setButtonType(btn.getType());
	// } else if (btn.getType() != null && contains(ButtonClicker.action_ids,
	// btn.getType())) {
	// ButtonClicker bc = new ButtonClicker();
	// bc.setActionID(btn.getType());
	// ((Button) ac).setButtonClicker(bc);
	// bc.setOpenPath(btn.getUrl());
	// // for (Parameter p : btn.getParas()) {
	// // p.setContainer();
	// // bc.addParameter(p);
	// // }
	// bc.setButton((Button) ac);
	// queryMap.put(btn.getId(), btn.getTarget());
	// }
	// }
	// }
	//
	// private boolean fillTabRef(AuroraComponent acpt, Component cp) {
	// if ((cp instanceof aurora.ide.meta.gef.editors.template.TabRef) && (acpt
	// instanceof TabItem)) {
	// TabRef ref = ((TabItem) acpt).getTabRef();
	// if (ref == null) {
	// ref = new TabRef();
	// }
	// String im = ((aurora.ide.meta.gef.editors.template.TabRef)
	// cp).getInitModel();
	// for (BMReference bm : template.getInitBms()) {
	// if (!im.equals(bm.getId())) {
	// continue;
	// }
	// String s = getBmPath(bm.getModel());
	// InitModel m = new InitModel();
	// m.setPath(s);
	// ref.setInitModel(m);
	// initModels.add(m);
	// }
	// ref.setOpenPath(((aurora.ide.meta.gef.editors.template.TabRef)
	// cp).getUrl());
	// ref.addAllParameter(((aurora.ide.meta.gef.editors.template.TabRef)
	// cp).getParas());
	// ((TabItem) acpt).setTabRef(ref);
	// return true;
	// }
	// return false;
	// }
	//
	// private String getBmPath(IFile bm) {
	// if (bm == null) {
	// return "";
	// }
	// String s = Util.toPKG(bm.getFullPath());
	// if (s.endsWith(".bm")) {
	// s = s.substring(0, s.lastIndexOf(".bm"));
	// }
	// return s;
	// }
	//
	// private boolean contains(String[] ss, String s) {
	// for (String st : ss) {
	// if (st.equals(s)) {
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// private void fillContainer(Component cpt, Container acpt) {
	// String bmId = ((BMBindComponent) cpt).getBmReferenceID();
	// if (bmId == null || "".equals(bmId)) {
	// return;
	// }
	// BMReference bm = null;
	// for (BMReference b : template.getBms()) {
	// if (!bmId.equals(b.getId())) {
	// continue;
	// }
	// bm = b;
	// break;
	// }
	// if (bm == null) {
	// return;
	// }
	// ((Container) acpt).setSectionType(Container.SECTION_TYPE_RESULT);
	// if (acpt instanceof Grid) {
	// fillGrid((Grid) acpt, bm.getModel());
	// ResultDataSet ds = ((Grid) acpt).getDataset();
	// String s = getBmPath(bm.getModel());
	// ds.setModel(s);
	// ((Container) acpt).setDataset(ds);
	// } else if (template.getType().equals(Template.TYPE_DISPLAY)) {
	// for (CommentCompositeMap map :
	// GefModelAssist.getFields(GefModelAssist.getModel(bm.getModel()))) {
	// Label label = new Label();
	// label.setName(map.getString("name"));
	// label.setPrompt(map.getString("prompt") == null ? map.getString("name") :
	// map.getString("prompt"));
	// if (GefModelAssist.getType(map) != null) {
	// label.setType(GefModelAssist.getType(map));
	// }
	// ((Container) acpt).addChild(label);
	// }
	// } else {
	// for (CommentCompositeMap map :
	// GefModelAssist.getFields(GefModelAssist.getModel(bm.getModel()))) {
	// Input input = new Input();
	// input.setName(map.getString("name"));
	// input.setPrompt(map.getString("prompt") == null ? map.getString("name") :
	// map.getString("prompt"));
	// if (GefModelAssist.getType(map) != null) {
	// input.setType(GefModelAssist.getType(map));
	// }
	// ((Container) acpt).addChild(input);
	// }
	// }
	// String qcId = ((BMBindComponent) cpt).getQueryComponent();
	// if (qcId != null && (!"".equals(qcId))) {
	// modelMap.put(qcId, bm.getModel());
	// queryMap.put(cpt.getId(), qcId);
	// }
	// }
	//
	// private void fillGrid(Grid grid, IFile bm) {
	// for (CommentCompositeMap map :
	// GefModelAssist.getFields(GefModelAssist.getModel(bm))) {
	// GridColumn gc = new GridColumn();
	// gc.setName(map.getString("name"));
	// gc.setPrompt(map.getString("prompt") == null ? map.getString("name") :
	// map.getString("prompt"));
	// if (GefModelAssist.getTypeNotNull(map) != null &&
	// (!template.getType().equals(Template.TYPE_DISPLAY))) {
	// gc.setEditor(GefModelAssist.getType(map));
	// }
	// grid.addCol(gc);
	// }
	// grid.setNavbarType(Grid.NAVBAR_COMPLEX);
	// grid.setSelectionMode(ResultDataSet.SELECT_MULTI);
	// }

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		if (page instanceof SelectModelWizardPage) {
			if (page.isPageComplete()) {
				return true;
			}
		} else if ((page instanceof SetLinkOrRefWizardPage) && page.isPageComplete()) {
			return true;
		}
		return false;
	}

	public boolean needsProgressMonitor() {
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
	}
}
