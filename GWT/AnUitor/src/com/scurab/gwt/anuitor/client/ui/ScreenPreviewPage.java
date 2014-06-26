package com.scurab.gwt.anuitor.client.ui;

import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.kiouri.sliderbar.client.event.BarValueChangedEvent;
import com.kiouri.sliderbar.client.event.BarValueChangedHandler;
import com.scurab.gwt.anuitor.client.DataProvider;
import com.scurab.gwt.anuitor.client.model.Pair;
import com.scurab.gwt.anuitor.client.model.ViewNodeHelper;
import com.scurab.gwt.anuitor.client.model.ViewNodeJSO;
import com.scurab.gwt.anuitor.client.style.CustomTreeResources;
import com.scurab.gwt.anuitor.client.util.CanvasTools;
import com.scurab.gwt.anuitor.client.util.CellTreeTools;
import com.scurab.gwt.anuitor.client.util.HTMLColors;
import com.scurab.gwt.anuitor.client.util.TableTools;
import com.scurab.gwt.anuitor.client.viewmodel.ViewHierarchyTreeViewModel;
import com.scurab.gwt.anuitor.client.viewmodel.ViewHierarchyTreeViewModel.OnSelectionChangedListener;
import com.scurab.gwt.anuitor.client.viewmodel.ViewHierarchyTreeViewModel.OnViewNodeMouseOverListener;
import com.scurab.gwt.anuitor.client.widget.ScaleSliderBar;

public class ScreenPreviewPage extends Composite {

    private static TestPageUiBinder uiBinder = GWT.create(TestPageUiBinder.class);
    @UiField Image image;    
    @UiField Label mousePosition;
    @UiField FlowPanel flowPanel;
    @UiField VerticalPanel centerPanel;
    @UiField Label hoveredViewID;    
    @UiField VerticalPanel topImagePanel;
    @UiField(provided=true) CellTable<Pair> cellTable = new CellTable<Pair>();

    /* Current color set for highlighting multiple views below the mouse cursor, currently disabled look for TAG_COLORS */
    private static final String[] COLORS = new String[] { HTMLColors.RED,HTMLColors.MAGENTA, HTMLColors.GREEN, HTMLColors.CYAN, HTMLColors.YELLOW, HTMLColors.BLUE};
    
    private static final boolean ZOOM_CANVAS_FEATURE = false;
    /* Zoom canvas scale constant */
    private static final double ZOOM_CANVAS_SCALE = 1.5;    
    /* Main canvas scale */
    private float mScale = 1;
    /* Main canvas widget */
    private Canvas mCanvas;
    /* Preview canvas widget */
    private Canvas mCanvasPreview;
    /* Zoomed canvas preview size, currently disabled */
    private int mCanvasPreviewSize = 250;
    
    
    /* Current screenshot dimensions */
    private int mImageWidth;
    private int mImageHeight;
    
    /* Scale slider bar */
    private ScaleSliderBar mScaleSliderBar;
    /* Minimum scale for canvas */
    private static final int SCALE_MIN = 30;//pct    
    /* DataSet for tree view hierarchy */
    private CellTree mCellTree;    
    /* Tree view model */
    private ViewHierarchyTreeViewModel mTreeViewModel;
    /* Hover timer */
    private MyTimer mTimer = new MyTimer();
    /* There is a selected view on screen */
    private boolean mSelectedView = false;
    /* Download root for view hierarchy */
    private ViewNodeJSO mRoot;

    interface TestPageUiBinder extends UiBinder<Widget, ScreenPreviewPage> {
    }

    public ScreenPreviewPage() {
        initWidget(uiBinder.createAndBindUi(this));
        image.setVisible(false);
        
        mScaleSliderBar = new ScaleSliderBar(200 - SCALE_MIN /* max(200) - min(30) = 170 */, "400px");               
        topImagePanel.insert(mScaleSliderBar, 0);
        
        bind();
        initTable();
        onReloadImage();                              
    }

    private void bind() {
        image.addLoadHandler(new LoadHandler() {
            @Override
            public void onLoad(LoadEvent event) {                
                mCanvas.setCoordinateSpaceWidth((mImageWidth = image.getWidth()));
                mCanvas.setCoordinateSpaceHeight((mImageHeight = image.getHeight()));
                int max = Window.getClientHeight() - 80;//top panel + margins
                //scale if height is bigger then window
                float scale = 1f;
                if(mImageHeight > max){
                    scale = max / (float)mImageHeight;
                    scale = Math.max(SCALE_MIN/100f, scale);                   
                }
                //update slider
                mScaleSliderBar.setValue((int)(((mScaleSliderBar.getMaxValue() + SCALE_MIN) / 2f) * scale) - SCALE_MIN);
                //finish loading and render
                reloadCanvas(scale);
            }
        }); 
        
        mScaleSliderBar.addBarValueChangedHandler(new BarValueChangedHandler() {            
            @Override
            public void onBarValueChanged(BarValueChangedEvent event) {
                int value = event.getValue() + SCALE_MIN;//min 30%
                mScale = value / 100f;
                updateImageSize(mScale);    
            }
        }); 
        
        mCanvas = Canvas.createIfSupported();
        if (mCanvas == null) {
            Window.alert("Canvas is not supported!?");
            return;
        }
        
        mCanvas.getElement().getStyle().setCursor(com.google.gwt.dom.client.Style.Cursor.CROSSHAIR);

        //add mouse move handler, handled view highlighting
        mCanvas.addMouseMoveHandler(new MouseMoveHandler() {
            @Override
            public void onMouseMove(MouseMoveEvent event) { 
                if(mSelectedView || mTreeViewModel != null && mTreeViewModel.getSelectedNode() != null){
                    return;//dont do anything here if we have selected node
                }
                int x = event.getRelativeX(mCanvas.getElement());
                int y = event.getRelativeY(mCanvas.getElement());
                int scaledX = (int) (x / mScale);
                int scaledY = (int) (y / mScale);
                
                onUpdateImageMousePosition(scaledX, scaledY);
                
                onUpdateZoomCanvas(scaledX, scaledY);
                if (mRoot != null) {
                    mTimer.schedule(scaledX, scaledY);
                }
            }
        });
        
        //mouse out handler just clears selection if necessary
        mCanvas.addMouseOutHandler(new MouseOutHandler() {                
            @Override
            public void onMouseOut(MouseOutEvent event) {
                if (!mSelectedView) {
                    clearCanvas();
                    if (mTreeViewModel != null) {
                        mTreeViewModel.clearHighlightedNode();
                    }
                }
            }
        });
        
        //click handler
        mCanvas.addMouseDownHandler(new MouseDownHandler() {                
            @Override
            public void onMouseDown(MouseDownEvent event) {               
                if (mTreeViewModel != null && mRoot != null) {
                    int x = event.getRelativeX(mCanvas.getElement());
                    int y = event.getRelativeY(mCanvas.getElement());                        
                    int scaledX = (int) (x / mScale);
                    int scaledY = (int) (y / mScale);
                    ViewNodeJSO vs = ViewNodeHelper.findFrontVisibleView(mRoot, scaledX, scaledY);
                    if (vs != null) {
                        if (vs == mTreeViewModel.getSelectedNode()) {
                            mTreeViewModel.clearSelectedNode();
                            mSelectedView = false;
                        } else {
                            mTreeViewModel.selectNode(vs);
                            mSelectedView = true;
                        }
                    }
                }
            }
        });
        
        //add canvas to document
        flowPanel.add(mCanvas);
        onInitZoomCanvas();       
    }        

    /**
     * Init zoom canvas feature, depends on {@link #ZOOM_CANVAS_FEATURE}
     */
    protected void onInitZoomCanvas() {
        if (!ZOOM_CANVAS_FEATURE) {
            return;
        }
        mCanvasPreview = Canvas.createIfSupported();
        mCanvasPreview.setCoordinateSpaceHeight(mCanvasPreviewSize);
        mCanvasPreview.setCoordinateSpaceWidth(mCanvasPreviewSize);
        centerPanel.add(mCanvasPreview);
    }

    /**
     * Redraw zoom canvas base on position
     * @param x
     * @param y
     */
    protected void onUpdateZoomCanvas(int x, int y) {
        if (mCanvasPreview == null) {
            return;
        }
        double scale = mScale * ZOOM_CANVAS_SCALE;
        Context2d c = mCanvasPreview.getContext2d();
        c.save();        
        c.scale(scale, scale);
        c.setFillStyle(HTMLColors.WHITE);
        c.fillRect(0, 0, mCanvasPreviewSize, mCanvasPreviewSize);
        int half = (int) ((mCanvasPreviewSize >> 1) / scale);
        c.drawImage(ImageElement.as(image.getElement()), -x + half, -y + half);
        c.restore();
    }

    /**
     * Reload canvas with scale = 1
     */
    private void reloadCanvas() {
        reloadCanvas(1f);
    }    
    
    /**
     * Reload canvas with specific scale
     * @param scale
     */
    private void reloadCanvas(float scale) {
        mScale = scale;
        updateImageSize(mScale);
    }

    /**
     * Clear canvas, all highlights will be removed
     */
    private void clearCanvas() {
        updateImageSize(mScale);
    }       
    
    /**
     * Init table widget
     */
    private void initTable(){
        TableTools.initTableForPairs(cellTable);                    
    }
    
    /**
     * Called when tree node has been changed
     * @param viewNode
     * @param selected
     */
    protected void onViewTreeNodeSelectionChanged(ViewNodeJSO viewNode, boolean selected){
        clearCanvas();
        mSelectedView = selected;
        if (selected) {            
            onShowTableDetail(viewNode);
            drawRectForView(viewNode);
        }        
    }        

    /**
     * Show table detail based on node view
     * @param viewNode
     */
    private void onShowTableDetail(ViewNodeJSO viewNode) {        
        TableTools.createDataProvider(viewNode).addDataDisplay(cellTable);
    }

    /**
     * Draw rectangle on canvas
     * @param c
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public static void drawRectangle(Canvas c, int x, int y, int w, int h) {
        CanvasTools.drawRectangle(c, x, y, w, h, HTMLColors.RED, HTMLColors.YELLOW);
    }
    
    /**
     * Draw rectangle on canvas to highlight view
     * @param view
     */
    private void drawRectForView(ViewNodeJSO view) {
        clearCanvas();
        drawRectForView(view, mCanvas, mScale, HTMLColors.RED, HTMLColors.YELLOW);
    }

    /**
     * Draw rectangle on canvas to highlight view with specific colors
     * @param view
     * @param canvas
     * @param scale
     * @param stroke
     * @param fill
     */
    private void drawRectForView(ViewNodeJSO view, Canvas canvas, float scale, String stroke, String fill) {
        if (view == null) {
            return;
        }       
        hoveredViewID.setText("ID:" + view.getID() + " Name:" + view.getIDName());
        CanvasTools.drawRectForView(view, canvas, scale, stroke, fill);
    }
   
    /**
     * Called when is necessary to update mouse position
     * @param x
     * @param y
     */
    protected void onUpdateImageMousePosition(int x, int y) {
        ImageData data = mCanvas.getContext2d().getImageData(x, y, 1, 1);
        String color = HTMLColors.getColorFromImageData(data);
        mousePosition.setText("X:" + x + " Y:" + y + " " + color);
    }
   
    /**
     * Called when image is necessary to reload
     */
    protected void onReloadImage() {        
        image.setUrl("/screen.png?time=" + System.currentTimeMillis()); //just to avoid caching
        loadTree();
    }       
        
    /**
     * Load tree view hierarchy
     */
    private void loadTree(){
        DataProvider.getTreeHierarchy(new DataProvider.AsyncCallback<ViewNodeJSO>() {
            @Override
            public void onError(Request r, Throwable t) {
                Window.alert(t.getMessage());
            }

            @Override
            public void onDownloaded(ViewNodeJSO result) {
                mRoot = result;                
                CellTree.Resources res = GWT.create(CustomTreeResources.class);
                //create tree model
                mTreeViewModel = new ViewHierarchyTreeViewModel(result);
                //add selection handler to select view on canvas
                mTreeViewModel.setOnSelectionChangedListener(new OnSelectionChangedListener() {                    
                    @Override
                    public void onSelectionChanged(ViewNodeJSO viewNode, boolean selected) {                        
                        ScreenPreviewPage.this.onViewTreeNodeSelectionChanged(viewNode, selected);
                    }
                });
                //add hover listener just to highlight view
                mTreeViewModel.setOnViewNodeMouseOverListener(new OnViewNodeMouseOverListener() {
                    @Override
                    public void onMouseOver(ViewNodeJSO viewNode) {
                        if (mTreeViewModel.getSelectedNode() == null) {
                            clearCanvas();
                            drawRectForView(viewNode);
                        }
                    }
                });
                //remove old tree if necessary, currently there is no reload support (reload page only)
                if (mCellTree != null) {
                    mCellTree.removeFromParent();
                }
                mCellTree = new CellTree(mTreeViewModel, null, res);       
                mCellTree.setDefaultNodeSize(1000);//no show more button
                centerPanel.add(mCellTree);                
                CellTreeTools.expandAll(mCellTree.getRootTreeNode());
                mCellTree.setAnimationEnabled(true);
            }                        
        });
    }    

    /**
     * Update image size based on scale
     * @param scale 1f = 100%
     */
    private void updateImageSize(float scale) {
        int w = (int) (mImageWidth * scale);
        int h = (int) (mImageHeight * scale);
        mCanvas.setCoordinateSpaceWidth(w);
        mCanvas.setCoordinateSpaceHeight(h);
        mCanvas.getContext2d().drawImage(ImageElement.as(image.getElement()), 0, 0, w, h);
    }      
    
    /**
     * Timer for little delay between mouse move and "hover", performance issue
     * @author jbruchanov
     *
     */
    private class MyTimer extends Timer {
        private int mX;
        private int mY;

        @Override
        public void run() {
            
            clearCanvas();
            if (true) {
                ViewNodeJSO vs = ViewNodeHelper.findFrontVisibleView(mRoot, mX, mY);
                drawRectForView(vs, mCanvas, mScale, HTMLColors.RED, COLORS[0]);
                if (mTreeViewModel != null) {
                    mTreeViewModel.highlightNode(vs);
                }
                return;
            }
            
            //disabled for now to show rest of views below a cursor TAG_COLORS
            List<ViewNodeJSO> views = ViewNodeHelper.findViewsByPosition(mRoot, mX, mY);
            
            for (int i = 0, n = views.size(); i < n; i++) {
                ViewNodeJSO v = views.get(i);                
                drawRectForView(v, mCanvas, mScale, HTMLColors.RED, COLORS[i % COLORS.length]);
            }
        }

        public void schedule(int x, int y) {
            cancel();
            mX = x;
            mY = y;
            schedule(5);
        }
    };
}
