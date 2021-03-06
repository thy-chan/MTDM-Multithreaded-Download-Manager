package cn.thychan.mtdm.ui;

import cn.thychan.mtdm.ContextHolder;
import cn.thychan.mtdm.DownloadContext;
import cn.thychan.mtdm.info.Info;
import cn.thychan.mtdm.navigation.*;
import cn.thychan.mtdm.object.Resource;
import cn.thychan.mtdm.state.Downloading;
import cn.thychan.mtdm.state.Failed;
import cn.thychan.mtdm.state.Finished;
import cn.thychan.mtdm.state.Pause;
import cn.thychan.mtdm.util.DateUtil;
import cn.thychan.mtdm.util.FileUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by cn_cx on 2016/12/8.
 */

/**
 * 主界面
 */
public class MainFrame extends JFrame {

    //导航树
    private NavigationTree navTree;
    //下载列表
    private DownloadTable downloadTable;
    //信息列表
    private JList infoJList;
    //工具栏
    private JToolBar toolBar = new JToolBar();
    //新任务界面
    private NewTaskDialog taskDialog;
    //关于界面
    private AboutDialog aboutDialog;
    //所有任务节点
    private TaskNode taskNode = new TaskNode();
    //正在下载节点
    private DownloadingNode downloadingNode = new DownloadingNode();
    //下载失败节点
    private FailNode failNode = new FailNode();
    //下载完成节点
    private FinishNode finishNode = new FinishNode();
    //当前用户浏览的节点
    private DownloadNode currentNode = taskNode;

    //信息列表的对象
    private final static String FILE_SIZE_TEXT = "文件大小: ";
    private final static String FILE_PATH_TEXT = "文件路径: ";
    private final static String DOWNLOAD_DATE_TEXT = "下载时间: ";
    private final static String RESOURCE_INFO_TEXT = "资源信息: ";
    private List<Info> infoList = new ArrayList<Info>();
    private Info fileSize = new Info(FILE_SIZE_TEXT);
    private Info filePath = new Info(FILE_PATH_TEXT);
    private Info downloadDate = new Info(DOWNLOAD_DATE_TEXT);
    private Info info = new Info(RESOURCE_INFO_TEXT);

    java.net.URL newTask_imgURL = MainFrame.class.getResource("/images/tool/new_task.png");
    private JButton newTask = create(new AbstractAction("新任务", new ImageIcon(newTask_imgURL)) {
        public void actionPerformed(ActionEvent e) {
            newTask();
        }
    });

    java.net.URL start_imgURL = MainFrame.class.getResource("/images/tool/begin.png");
    private JButton start = create(new AbstractAction("开始", new ImageIcon(start_imgURL)) {
        public void actionPerformed(ActionEvent e) {
            start();
        }
    });

    java.net.URL pause_imgURL = MainFrame.class.getResource("/images/tool/pause.png");
    private JButton pause = create(new AbstractAction("暂停", new ImageIcon(pause_imgURL)) {
        public void actionPerformed(ActionEvent e) {
            pause();
        }
    });

    java.net.URL delete_imgURL = MainFrame.class.getResource("/images/tool/delete.png");
    private JButton delete = create(new AbstractAction("删除任务", new ImageIcon(delete_imgURL)) {
        public void actionPerformed(ActionEvent e) {
            delete();
        }
    });

    java.net.URL deleteFinished_imgURL = MainFrame.class.getResource("/images/tool/remove.png");
    private JButton deleteFinished = create(new AbstractAction("移除任务", new ImageIcon(deleteFinished_imgURL)) {
        public void actionPerformed(ActionEvent e) {
            deleteFinished();
        }
    });

    java.net.URL about_imgURL = MainFrame.class.getResource("/images/tool/about.png");
    private JButton about = create(new AbstractAction("关 于", new ImageIcon(about_imgURL)) {
        public void actionPerformed(ActionEvent e) {
            about();
        }
    });

    ActionListener refreshTable = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            //刷新列表
            downloadTable.updateUI();
        }
    };

    /**
     * 创建JButton
     * @param action
     * @return
     */
    static JButton create(Action action) {
        JButton button = new JButton(action);
        button.setVerticalTextPosition(JButton.BOTTOM);
        button.setHorizontalTextPosition(JButton.CENTER);
        return button;
    }

    public MainFrame() {
        //创建导航树
        createTree();
        createDownloadTable();
        //创建信息列表
        createList();
        this.taskDialog = new NewTaskDialog();
        this.aboutDialog = new AboutDialog();
        //创建工具栏
        createToolBar();
        //设置主窗口大小
        Dimension screen = new Dimension(1000,600);
        //导航滚动
        JScrollPane navPane = new JScrollPane(this.navTree);
        JScrollPane downloadPane = new JScrollPane(this.downloadTable);
        int downloadPaneHeight = (int)(screen.height/1.5);
        int downloadPaneWidth = (int)(screen.width/0.8);
        downloadPane.setPreferredSize(new Dimension(downloadPaneWidth, downloadPaneHeight));
        JScrollPane infoPane = new JScrollPane(this.infoJList);
        //主界面右边的分隔Pane
        JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                downloadPane, infoPane);
        rightPane.setDividerLocation(360);
        rightPane.setDividerSize(3);
        //主界面的分隔Pane
        JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                navPane, rightPane);
        mainPane.setDividerSize(3);
        mainPane.setDividerLocation((int)(screen.width/5.5));

        //修改标题栏默认图标
        Toolkit tk=Toolkit.getDefaultToolkit();
        java.net.URL icon_imgURL = MainFrame.class.getResource("/images/icon/chan.jpg");
        Image icon=tk.createImage(icon_imgURL);
        this.setIconImage(icon);

        this.add(mainPane);
        this.setPreferredSize(new Dimension(screen.width, screen.height));
        this.setVisible(true);
        this.setTitle("MTDM");
        this.pack();
        Dimension lScreen = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((int)(lScreen.width/4), (int)(lScreen.height/4));
        initlisteners();
        //创建定时器
        Timer timer = new Timer(1000, refreshTable);
        timer.start();
        //读取序列化文件
        reverseSer();
    }

    public NewTaskDialog getNewTaskFrame() {
        return this.taskDialog;
    }

    public AboutDialog getAboutDialog(){
        return this.aboutDialog;
    }

    private void initlisteners() {
        //点击列表鼠标监听器
        this.downloadTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                //得到点击的资源
                Resource r = getResource();
                if (r == null) return;
                //设置信息显示区域的值
                fileSize.setValue(FILE_SIZE_TEXT + r.getSize());
                filePath.setValue(FILE_PATH_TEXT +
                        r.getSaveFile().getAbsolutePath());
                downloadDate.setValue(DOWNLOAD_DATE_TEXT +
                        DateUtil.formatDate(r.getDownloadDate()));
                info.setValue(RESOURCE_INFO_TEXT + r.getState().getState());
                //重新设置JList数据
                infoJList.setListData(infoList.toArray());
            }
        });
        //点击导航树鼠标监听器
        this.navTree.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e) {
                selectTree();
            }
        });
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    /**
     * 点击导航树触发的方法
     */
    private void selectTree() {
        DownloadNode selectNode = getSelectNode();
        this.currentNode = selectNode;
        refreshTable();
    }

    /**
     * 刷新列表
     */
    private void refreshTable() {
        DownloadTableModel model = (DownloadTableModel)this.downloadTable.getModel();
        model.setResources(ContextHolder.ctx.getResources(currentNode));
    }

    private DownloadNode getSelectNode() {
        TreePath treePath = this.navTree.getSelectionPath();
        if (treePath == null) return null;
        //获得选中的TreeNode
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
        return (DownloadNode)node.getUserObject();
    }

    private void addTableData() {
        DownloadTableModel model = (DownloadTableModel)this.downloadTable.getModel();
        //将保存的资源设置到列表中
        model.setResources(ContextHolder.ctx.resources);
        //刷新列表
        this.downloadTable.refresh();
    }

    /**
     * 反序列化
     */
    public void reverseSer() {
        File serFile = FileUtil.SERIALIZABLE_FILE;
        if (!serFile.exists()) return;
        try {
            //得到文件输入流
            FileInputStream fis = new FileInputStream(serFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            //设置ContextHolder的DownloadContext
            ContextHolder.ctx = (DownloadContext)ois.readObject();
            ois.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //设置列表
        addTableData();
    }

    /**
     * 序列化(DownloadContext对象)
     */
    public void serializable() {
        try {
            //序列化前先将所有正在下载的任务停止
            for (Resource r : ContextHolder.ctx.resources) {
                if (r.getState() instanceof Downloading) {
                    r.setState(ContextHolder.ctx.PAUSE);
                }
            }
            File serFile = FileUtil.SERIALIZABLE_FILE;
            //判断序列化文件是否存在, 不存在则创建
            if (!serFile.exists()) serFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(serFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            //将上下文对象写到序列化文件中
            oos.writeObject(ContextHolder.ctx);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JTable getDownloadTable() {
        return this.downloadTable;
    }

    private void createDownloadTable() {
        DownloadTableModel tableModel = new DownloadTableModel();
        this.downloadTable = new DownloadTable();
        this.downloadTable.setModel(tableModel);
        this.downloadTable.setTableFace();
    }

    private void createToolBar() {
        this.toolBar.setFloatable(false);
        this.toolBar.add(this.newTask);
        this.toolBar.add(this.start);
        this.toolBar.add(this.pause);
        this.toolBar.add(this.delete);
        this.toolBar.add(this.deleteFinished);
        this.toolBar.add(this.about);

        this.toolBar.setMargin(new Insets(4, 10, 5, 10));
        this.add(this.toolBar, BorderLayout.NORTH);

    }

    /**
     * 新建任务
     */
    private void newTask() {
        this.taskDialog.setVisible(true);
    }

    /**
     * 开始下载
     */
    private void start() {
        Resource r = getResource();
        if (r == null) return;
        if (r.getState() instanceof Pause || r.getState() instanceof Failed) {
            ContextHolder.dh.resumeDownload(r);
        }
    }

    /**
     * 暂停下载
     */
    private void pause() {
        Resource r = getResource();
        if (r == null) return;
        //判断状态
        if (!(r.getState() instanceof Downloading)) return;
        r.setState(ContextHolder.ctx.PAUSE);
    }

    /**
     * 删除资源
     */
    private void delete() {
        Resource r = getResource();
        if (r == null) return;
        //先将任务停止
        r.setState(ContextHolder.ctx.PAUSE);
        //删除所有的.part文件
        FileUtil.deletePartFiles(r);
        //从上下文集合中删除资源
        ContextHolder.ctx.resources.remove(r);
    }

    /**
     * 删除已下载完成的资源
     */
    public void deleteFinished() {
        for (Iterator it = ContextHolder.ctx.resources.iterator(); it.hasNext();) {
            Resource r = (Resource)it.next();
            if (r.getState() instanceof Finished) {
                it.remove();
            }
        }
    }

    /**
     * 关于
     */
    public void about(){
        this.aboutDialog.setVisible(true);
    }

    /**
     * 得到用户在列表中所选择的资源
     * @return
     */
    private Resource getResource() {
        int row = this.downloadTable.getSelectedRow();
        int column = this.downloadTable.getColumn(DownloadTableModel.ID_COLUMN).getModelIndex();
        if (row == -1) return null;
        String id = (String)this.downloadTable.getValueAt(row, column);
        return ContextHolder.ctx.getResource(id);
    }

    /**
     * 创建树
     */
    private void createTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode tn = new DefaultMutableTreeNode(taskNode);
        root.add(tn);
        //创建各个节点
        tn.add(new DefaultMutableTreeNode(downloadingNode));
        tn.add(new DefaultMutableTreeNode(failNode));
        tn.add(new DefaultMutableTreeNode(finishNode));
        this.navTree = new NavigationTree(root);
    }

    private void createList() {
        this.infoJList = new JList();
        this.infoList.add(this.fileSize);
        this.infoList.add(this.filePath);
        this.infoList.add(this.downloadDate);
        this.infoList.add(this.info);
        this.infoJList.setListData(infoList.toArray());
    }
}