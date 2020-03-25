package org.hy.common.hbase;

import java.util.ArrayList;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.FamilyFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FilterList.Operator;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.ValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.hy.common.CounterMap;
import org.hy.common.Help;
import org.hy.common.Return;





/**
 * HBase数据库访问
 * 
 * @author ZhengWei(HY)
 * @create 2014-05-16
 *         2018-05-23  添加  表失效方法disableTable()
 */
public class HBase implements Cloneable
{
    
    /** 
     * 定义本类返回数据结果集的数据类型。
     * 
     * 它决定 getRows() 等相关方法返回类型 Map<String ,Map<String ,Object> 中 Object的类型。
     */
    public enum ResultType
    {
        /** 返回数据类型为字符串。默认类型 */
        ResultType_String
        
        /** 返回数据类型为HData */
       ,ResultType_HData
    }
    
    
    
	private Configuration              configuration;
	
	private HBaseAdmin                 hbaseAdmin;
	
	private Map<String ,HTable>        tables;
	
	/** 只用于获取表记录数时的分页处理。为了性能 */
	private long                       countPageSize = 100;
	
	/** 行数据是否有顺序 */
	private boolean                    isRowOrder    = true;
	
	/** 列数据是否有顺序 */
	private boolean                    isColOrder    = false;
	
	/** 数据类型 */
	private ResultType                 resultType    = ResultType.ResultType_String;
	
	
	
	/**
	 * 
     * @param host        HBase服务IP
     * @param znode       zookeeper node path
	 */
	public HBase(String host, String znode)
	{
	    this.configuration = new Configuration();
	    this.configuration.set("hbase.zookeeper.quorum" ,host); // HConstants.ZOOKEEPER_QUORUM
        this.configuration.set("zookeeper.znode.parent" ,znode);
	    this.configuration.set("hbase.client.pause", "100"); 
	    this.configuration.set("hbase.client.retries.number", "10"); 
	    this.configuration.set("hbase.rpc.timeout", "10000"); 
	    this.configuration.set("hbase.client.operation.timeout", "10000"); 
	    this.configuration.set("hbase.client.scanner.timeout.period", "10000");
		this.configuration = HBaseConfiguration.create(this.configuration);
		
		this.tables = new Hashtable<String, HTable>();
	}
	
	
	
	/**
	 * 
	 * @param i_IP        HBase服务IP
	 * @param i_Configs   HBase服务配置信息
	 */
	public HBase(String i_IP ,Map<String ,String> i_Configs)
	{
	    this(i_IP, "");
	    
	    if ( !Help.isNull(i_Configs) )
	    {
	        Iterator<String> v_Keys = i_Configs.keySet().iterator();
	        
    	    while ( v_Keys.hasNext() )
    	    {
    	        String v_Key = v_Keys.next();
    	        
    	        this.configuration.set(v_Key ,i_Configs.get(v_Key));
    	    }
	    }
	}
	
	
	
	/**
	 * 
	 * @param i_Configuration
	 */
	public HBase(Configuration i_Configuration)
	{
	    if ( i_Configuration == null )
	    {
	        throw new NullPointerException("Configuration is null.");
	    }
	    
	    this.configuration = new Configuration(i_Configuration);
        this.configuration = HBaseConfiguration.create(this.configuration);
        
        this.tables = new Hashtable<String, HTable>();
	}
	
	
	
	@Override
    protected void finalize() throws Throwable
    {
	    this.tables.clear();
        super.finalize();
    }
	
	
    
    @Override
    public HBase clone()
    {
        HBase v_Clone = new HBase(this.configuration);
        
        v_Clone.setRowOrder(     this.isRowOrder());
        v_Clone.setColOrder(     this.isColOrder());
        v_Clone.setCountPageSize(this.getCountPageSize());
        v_Clone.setResultType(   this.getResultType());
        
        return v_Clone;
    }

    
    
    /**
     * 获取数据库IP或主机名
     * 
     * @return
     */
    public String getHBaseIP()
    {
        return this.configuration.get("hbase.zookeeper.quorum");
    }

    

    public long getCountPageSize()
    {
        return countPageSize;
    }

    
    
    public void setCountPageSize(long i_GetCountPageSize)
    {
        this.countPageSize = (new HPage(i_GetCountPageSize)).getPageSize();
    }
    
    
    
    public boolean isRowOrder()
    {
        return isRowOrder;
    }

    
    
    public void setRowOrder(boolean isRowOrder)
    {
        this.isRowOrder = isRowOrder;
    }

    
    
    public boolean isColOrder()
    {
        return isColOrder;
    }
    

    
    public void setColOrder(boolean isColOrder)
    {
        this.isColOrder = isColOrder;
    }
    
    
    
    public ResultType getResultType()
    {
        return resultType;
    }
    

    
    public void setResultType(ResultType i_ResultType)
    {
        if ( i_ResultType == null )
        {
            this.resultType = ResultType.ResultType_String;
        }
        else
        {
            this.resultType = i_ResultType;
        }
    }
    
    
    
    public void setResultType_HData()
    {
        this.resultType = ResultType.ResultType_HData;
    }
    
    
    
    private Map<String ,Map<String ,Object>> newRowMap()
    {
        if ( this.isRowOrder )
        {
            return new LinkedHashMap<String ,Map<String ,Object>>();
        }
        else
        {
            return new HashMap<String ,Map<String ,Object>>();
        }
    }
    
    
    
    private Map<String ,Object> newColMap()
    {
        if ( this.isRowOrder )
        {
            return new LinkedHashMap<String ,Object>();
        }
        else
        {
            return new HashMap<String ,Object>();
        }
    }
    
    
    
    /**
     * 获取管理对象
     * 
     * @return
     * @throws IOException 
     * @throws ZooKeeperConnectionException 
     * @throws MasterNotRunningException 
     */
    public synchronized HBaseAdmin getHBaseAdmin() throws MasterNotRunningException, ZooKeeperConnectionException, IOException
    {
        if ( this.hbaseAdmin == null )
        {
            this.hbaseAdmin = new HBaseAdmin(this.configuration);
        }
        
        return this.hbaseAdmin;
    }
    
    
    
    /**
     * 获取表对象
     * 
     * @param i_TableName
     * @return
     * @throws IOException
     */
    public synchronized HTable getTable(String i_TableName)
    {
        HTable v_Table = null;
        
        if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Table name is null.");
        }
        
        
        String v_TableName = i_TableName.trim();
        
        if ( this.tables.containsKey(v_TableName) )
        {
            v_Table = this.tables.get(v_TableName);
        }
        else
        {
            if ( this.isExistsTable(v_TableName) )
            {
                try
                {
                    v_Table = new HTable(this.configuration ,v_TableName);
                
                    this.tables.put(v_TableName ,v_Table);
                }
                catch (Exception exce)
                {
                    exce.printStackTrace();
                }
            }
            else
            {
                throw new NullPointerException("Table[" + v_TableName + "] is not exists.");
            }
        }
        
        return v_Table;
    }
    
    
    
    /**
     * 获取数据库所有表名称
     * 
     * @return
     */
    public List<String> getTableNames()
    {
        List<String> v_Ret = new ArrayList<String>();
        
        try
        {
            TableName [] v_TableNames = this.getHBaseAdmin().listTableNames();
            
            if ( v_TableNames != null )
            {
                for (int i=0; i<v_TableNames.length; i++)
                {
                    v_Ret.add(v_TableNames[i].getNameAsString());
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 检查表是否存在
     * 
     * @param i_TableName
     * @return
     */
    public boolean isExistsTable(String i_TableName)
    {
        if ( Help.isNull(i_TableName) )
        {
            return false;
        }
        else
        {
            try
            {
                return this.getHBaseAdmin().tableExists(i_TableName.trim());
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
                return false;
            }
        }
    }
    
    
    
    /**
     * 获取表的所有列族名称
     * 
     * @return
     */
    public List<String> getTableFamilyNames(String i_TableName)
    {
        Collection<HColumnDescriptor> v_HColumns = this.getTableFamilys(i_TableName);
        List<String>                  v_Ret      = new ArrayList<String>();
        
        if ( !Help.isNull(v_HColumns) )
        {
            for (HColumnDescriptor v_HColumn : v_HColumns)
            {
                v_Ret.add(v_HColumn.getNameAsString());
            }
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 获取列簇信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @param i_TableName   表名称
     * @param i_FamilyName  列簇名称
     * @return
     */
    public HColumnDescriptor getTableFamily(String i_TableName ,String i_FamilyName)
    {
        HTable v_HTable = this.getTable(i_TableName);
        
        try
        {
            HTableDescriptor v_HTableDescriptor = v_HTable.getTableDescriptor();
            return v_HTableDescriptor.getFamily(Bytes.toBytes(i_FamilyName));
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return null;
    }
    
    
    
    /**
     * 获取表的所有列族信息
     * 
     * @return
     */
    public Collection<HColumnDescriptor> getTableFamilys(String i_TableName)
    {
        HTable v_HTable = this.getTable(i_TableName);
        
        try
        {
            HTableDescriptor v_HTableDescriptor = v_HTable.getTableDescriptor();
            return v_HTableDescriptor.getFamilies();
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return null;
    }
    
    
    
    /**
     * 表失效
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-05-23
     * @version     v1.0
     *
     * @param i_TableName
     */
    public void disableTable(String i_TableName)
    {
        if ( this.isExistsTable(i_TableName) )
        {
            try
            {
                this.getHBaseAdmin().disableTable(i_TableName.trim());
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        else
        {
            throw new NullPointerException("Disable table[" + i_TableName + "] is not exists.");
        }
    }
    
    
    
    /**
     * 删除表
     * 
     * @param i_TableName
     */
    public void dropTable(String i_TableName)
    {
        if ( this.isExistsTable(i_TableName) )
        {
            try
            {
                this.getHBaseAdmin().disableTable(i_TableName.trim());
                this.getHBaseAdmin().deleteTable( i_TableName.trim());
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        else
        {
            throw new NullPointerException("Drop table[" + i_TableName + "] is not exists.");
        }
    }
    
    
    
    /**
     * 清空表数据
     * 
     * 实际是Drop表后重建
     * 
     * @param i_TableName    表名称
     */
    public void truncate(String i_TableName)
    {
        if ( this.isExistsTable(i_TableName) )
        {
            try
            {
                HTableDescriptor v_HTableDesc = this.getTable(i_TableName).getTableDescriptor();
                
                this.getHBaseAdmin().disableTable(i_TableName.trim());
                this.getHBaseAdmin().deleteTable( i_TableName.trim());
                this.getHBaseAdmin().createTable( v_HTableDesc);
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        else
        {
            throw new NullPointerException("Truncate table[" + i_TableName + "] is not exists.");
        }
    }
    
    
    
    /**
     * 创建表
     * 
     * @param i_TableName    表名称
     * @param i_FamilyNames  列族名称集合
     */
    public void createTable(String i_TableName ,List<String> i_FamilyNames)
    {
        this.core_createTable(i_TableName ,i_FamilyNames.toArray(new String[]{}));
    }
    
    
    
    /**
     * 创建表
     * 
     * @param i_TableName    表名称
     * @param i_FamilyNames  列族名称集合
     */
    public void createTable(String i_TableName ,String ... i_FamilyNames)
    {
        this.core_createTable(i_TableName ,i_FamilyNames);
    }
    
    
    
    /**
     * 创建表
     * 
     * @param i_TableName        表名称
     * @param i_FamilyNames      列族名称
     * @param i_FamilyBlockSize  数据块的大小(单位：字节)
     */
    public void createTable(String i_TableName ,String i_FamilyName ,int i_FamilyBlockSize)
    {
        HColumnDescriptor v_HColumnDescriptor = new HColumnDescriptor(i_FamilyName.trim());
        
        v_HColumnDescriptor.setBlocksize(i_FamilyBlockSize);
        
        this.createTable(i_TableName ,v_HColumnDescriptor);
    }
    
    
    
    /**
     * 创建表
     * 
     * @param i_TableName    表名称
     * @param i_Familys      列族对象集合
     */
    public void createTable(String i_TableName ,HColumnDescriptor ... i_Familys)
    {
        this.core_createTable(i_TableName ,i_Familys);
    }
    
    
    
    /**
     * 创建表
     * 
     * @param i_TableName    表名称
     * @param i_FamilyNames  列族名称集合
     */
    private void core_createTable(String i_TableName ,String [] i_FamilyNames)
    {
        if ( Help.isNull(i_FamilyNames) )
        {
            throw new NullPointerException("Create table[" + i_TableName + "] FamilyNames is null.");
        }
        
        HColumnDescriptor [] v_Familys = new HColumnDescriptor[i_FamilyNames.length];
        
        for (int i=0; i<i_FamilyNames.length; i++)
        {
            if ( i_FamilyNames[i] == null || Help.isNull(i_FamilyNames[i]) )
            {
                throw new NullPointerException("Create table[" + i_TableName + "] FamilyNames[" + i +"] is null.");
            }
            
            v_Familys[i] = new HColumnDescriptor(i_FamilyNames[i].trim());
        }
        
        this.core_createTable(i_TableName ,v_Familys);
    }
    

    
    /**
     * 创建表
     * 
     * HBase存在数据块限制，需要根据应用进行调整。
     * 默认情况下，数据块限制为64KB。
     * 由于图片内容作为单元格(Cell)的值保存，其大小受制于数据块的大小。
     * 在应用中需根据最大图片大小对HBase数据块大小进行修改。
     * 用HColumnDescriptor.setBlocksize指定数据块大小，可分列簇指定。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-13
     * @version     v1.0
     *
     * @param i_TableName    表名称
     * @param i_Familys      列族对象集合
     */
    private void core_createTable(String i_TableName ,HColumnDescriptor [] i_Familys)
    {
        if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Create table name is null.");
        }
        
        if ( Help.isNull(i_Familys) )
        {
            throw new NullPointerException("Create table[" + i_TableName + "] FamilyNames is null.");
        }
        
        
        HTableDescriptor v_HTableDesc = new HTableDescriptor(TableName.valueOf(i_TableName.trim()));
        
        for (int i=0; i<i_Familys.length; i++)
        {
            if ( i_Familys[i] == null || Help.isNull(i_Familys[i].getNameAsString()) )
            {
                throw new NullPointerException("Create table[" + i_TableName + "] FamilyNames[" + i +"] is null.");
            }
            
            v_HTableDesc.addFamily(i_Familys[i]);
        }
        
        try
        {
            this.getHBaseAdmin().createTable(v_HTableDesc);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    /**
     * 向表中添加新的列族名
     * 
     * @param i_TableName
     * @param i_FamilyName
     */
    public void addFamily(String i_TableName ,String i_FamilyName)
    {
        if ( Help.isNull(i_FamilyName) )
        {
            throw new NullPointerException("Add fmaily name is null.");
        }
        
        try
        {
            if ( this.isExistsTable(i_TableName) )
            {
                this.getHBaseAdmin().addColumn(i_TableName.trim() ,new HColumnDescriptor(i_FamilyName.trim()));
            }
            else
            {
                throw new NullPointerException("Add fmaily table[" + i_TableName + "] is not exists.");
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    /**
     * 删除表中的列族名
     * 
     * @param i_TableName
     * @param i_FamilyName
     */
    public void dropFamily(String i_TableName ,String i_FamilyName)
    {
        if ( Help.isNull(i_FamilyName) )
        {
            throw new NullPointerException("Drop fmaily name is null.");
        }
        
        try
        {
            if ( this.isExistsTable(i_TableName) )
            {
                this.getHBaseAdmin().deleteColumn(i_TableName.trim() ,i_FamilyName.trim());
            }
            else
            {
                throw new NullPointerException("Drop fmaily table[" + i_TableName + "] is not exists.");
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    /**
     * 获取表结构
     * 
     * 采用探测的方式
     * 
     * @param i_TableName   表名称
     * @return
     */
    public CounterMap<String> getTableStructure(String i_TableName)
    {
        return this.getTableStructure(i_TableName ,1000);
    }
    
    
    
    /**
     * 获取表结构
     * 
     * 采用探测的方式
     * 
     * @param i_TableName   表名称
     * @param i_ScanCount   探测深度。即行数
     * @return
     */
    public CounterMap<String> getTableStructure(String i_TableName ,int i_ScanCount)
    {
        if ( this.getResultType() != ResultType.ResultType_HData )
        {
            throw new ClassCastException("ResultType must 'ResultType.ResultType_HData'.");
        }
        
        Map<String ,Map<String ,Object>> v_Ret       = this.getRows(i_TableName ,new HPage(i_ScanCount));
        CounterMap<String>               v_Structure = new CounterMap<String>();
        
        if ( !Help.isNull(v_Ret) )
        {
            for (String v_RowKey : v_Ret.keySet())
            {
                Map<String ,Object> v_RowInfo = v_Ret.get(v_RowKey);
                
                for (String v_ColName :v_RowInfo.keySet())
                {
                    HData v_HData = (HData)v_RowInfo.get(v_ColName);
                    
                    v_Structure.put(v_HData.getFamilyName() + ":" + v_HData.getColumnName());
                }
            }
        }
        
        return v_Structure;
    }
    
    
    
    /**
     * 插入一行记录中一个字段信息
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @param i_FamilyName   列族
     * @param i_ColumnName   字段名
     * @param i_Value        字段值 
     */
    public void add(String i_TableName ,String i_RowKey ,String i_FamilyName ,String i_ColumnName ,Object i_Value)
    {
        this.put(i_TableName ,i_RowKey ,i_FamilyName ,i_ColumnName ,i_Value);
    }
    
    
    
    /**
     * 插入一行记录中一个字段信息
     * 
     * 优化：当 i_HDatas 中多个元素的 RowKey 相同时，按一行完整记录插入
     * 
     * @param i_TableName    表名
     * @param i_HDatas       数据集合
     */
    public void add(String i_TableName ,HData ... i_HDatas)
    {
        this.put(i_TableName ,i_HDatas);
    }
    
    
    
    /**
     * 插入一行记录中一个字段信息
     * 
     * 优化：当 i_HDatas 中多个元素的 RowKey 相同时，按一行完整记录插入
     * 
     * @param i_TableName    表名
     * @param i_HDatas       数据集合
     */
    public void add(String i_TableName ,List<HData> i_HDatas)
    {
        this.put(i_TableName ,i_HDatas);
    }
    
    
    
    /**
     * 更新一行记录中一个字段信息
     * 
     * 注意：1. 先删除，后插入。只保存时间戳上的一个最新版本
     *       2. 如果删除的字段信息不存在，也不会造成异常的，可放心使用。
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @param i_FamilyName   列族
     * @param i_ColumnName   字段名
     * @param i_Value        字段值 
     */
    public void update(String i_TableName ,String i_RowKey ,String i_FamilyName ,String i_ColumnName ,Object i_Value)
    {
        this.delete(i_TableName ,i_RowKey ,i_FamilyName ,i_ColumnName);
        this.put(   i_TableName ,i_RowKey ,i_FamilyName ,i_ColumnName ,i_Value);
    }
    
    
    
    /**
     * 更新一行记录中一个字段信息
     * 
     * 注意：1. 先删除，后插入。只保存时间戳上的一个最新版本
     *       2. 如果删除的字段信息不存在，也不会造成异常的，可放心使用
     * 
     * 优化：当 i_HDatas 中多个元素的 RowKey 相同时，按一行完整记录更新
     * 
     * @param i_TableName    表名
     * @param i_HDatas       数据集合
     */
    public void update(String i_TableName ,HData ... i_HDatas)
    {
        this.delete(i_TableName ,i_HDatas);
        this.put(   i_TableName ,i_HDatas);
    }
    
    
    
    /**
     * 更新一行记录中一个字段信息
     * 
     * 注意：1. 先删除，后插入。只保存时间戳上的一个最新版本
     *       2. 如果删除的字段信息不存在，也不会造成异常的，可放心使用
     * 
     * 优化：当 i_HDatas 中多个元素的 RowKey 相同时，按一行完整记录更新
     * 
     * @param i_TableName    表名
     * @param i_HDatas       数据集合
     */
    public void update(String i_TableName ,List<HData> i_HDatas)
    {
        this.delete(i_TableName ,i_HDatas);
        this.put(   i_TableName ,i_HDatas);
    }
    
    
    
    /**
     * 更新一行记录中同一列族的多个字段信息
     * 
     * 注意：1. 先删除，后插入。只保存时间戳上的一个最新版本
     *       2. 如果删除的字段信息不存在，也不会造成异常的，可放心使用
     * 
     *  优化：同行多个字段更新，只操作一次 I/O
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @param i_FamilyName   列族
     * @param i_Columns      字段集合
     */
    public void update(String i_TableName ,String i_RowKey ,String i_FamilyName ,Map<String ,Object> i_Columns)
    {
        this.delete(i_TableName ,i_RowKey ,i_FamilyName ,new ArrayList<String>(i_Columns.keySet()));
        this.put(   i_TableName ,i_RowKey ,i_FamilyName ,i_Columns);
    }
    
    
    
    /**
     * 插入或更新一行记录中同一列族的多个字段信息
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @param i_FamilyName   列族
     * @param i_Columns      字段集合
     */
    public void put(String i_TableName ,String i_RowKey ,String i_FamilyName ,Map<String ,Object> i_Columns)
    {
        if ( Help.isNull(i_Columns) )
        {
            throw new NullPointerException("Call method[put] Columns is null.");
        }
        
        
        List<HData> v_HDatas = new ArrayList<HData>();
        
        for (String v_ColumnName : i_Columns.keySet())
        {
            v_HDatas.add(new HData(i_RowKey ,v_ColumnName ,i_Columns.get(v_ColumnName)).setFamilyName(i_FamilyName));
        }
        
        this.core_Put(i_TableName ,v_HDatas.toArray(new HData []{}));
    }
    
    

    /**
	 * 插入或更新一行记录中一个字段信息
	 * 
	 * @param i_TableName    表名
	 * @param i_RowKey       行主键
	 * @param i_FamilyName   列族
	 * @param i_ColumnName   字段名
	 * @param i_Value        字段值 
	 */
	public void put(String i_TableName ,String i_RowKey ,String i_FamilyName ,String i_ColumnName ,Object i_Value)
	{
	    this.put(i_TableName ,new HData(i_RowKey ,i_ColumnName ,i_Value).setFamilyName(i_FamilyName));
	}
	
	
	
	/**
     * 插入或更新一行记录中一个字段信息
     * 
     * 优化：当 i_HDatas 中多个元素的 RowKey 相同时，按一行完整记录插入或更新
     * 
     * @param i_TableName    表名
     * @param i_HDatas       数据集合
     */
	public void put(String i_TableName ,HData ... i_HDatas)
	{
	    this.core_Put(i_TableName ,i_HDatas);
	}
	
	
	
	/**
     * 插入或更新一行记录中一个字段信息
     * 
     * 优化：当 i_HDatas 中多个元素的 RowKey 相同时，按一行完整记录插入或更新
     * 
     * @param i_TableName    表名
     * @param i_HDatas       数据集合
     */
    public void put(String i_TableName ,List<HData> i_HDatas)
    {
        if ( Help.isNull(i_HDatas) )
        {
            throw new NullPointerException("Call method[put] HDatas is null.");
        }
        
        this.core_Put(i_TableName ,i_HDatas.toArray(new HData []{}));
    }
	
	
	
	/**
     * 插入或更新一行记录中一个字段信息
     * 
     * 优化：当 i_HDatas 中多个元素的 RowKey 相同时，按一行完整记录插入或更新
     * 
     * @param i_TableName    表名
     * @param i_HDatas       数据集合
     */
	private void core_Put(String i_TableName ,HData [] i_HDatas)
	{
	    if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Call method[put] TableName is null.");
        }
        
        if ( i_HDatas == null || i_HDatas.length <= 0 )
        {
            throw new NullPointerException("Call method[put] HDatas is null.");
        }
        
        
        HTable           v_Table = null;
        Map<String ,Put> v_Puts  = new Hashtable<String ,Put>();
        
        for (int i=0; i<i_HDatas.length; i++)
        {
            HData v_HData = i_HDatas[i];
            
            if ( v_HData == null )
            {
                throw new NullPointerException("Call method[put] HDatas[" + i + "] is null.");
            }
            
            if ( Help.isNull(v_HData.getRowKey()) )
            {
                throw new NullPointerException("Call method[put] HDatas[" + i + "].getRowKey() is null.");
            }
            
            if ( Help.isNull(v_HData.getFamilyName()) )
            {
                throw new NullPointerException("Call method[put] HDatas[" + i + "].getFamilyName() is null.");
            }
            
            if ( Help.isNull(v_HData.getColumnName()) )
            {
                throw new NullPointerException("Call method[put] HDatas[" + i + "].getColumnName() is null.");
            }
            
            if ( v_HData.getValue() == null )
            {
                throw new NullPointerException("Call method[put] HDatas[" + i + "].getValue() is null.");
            }
            
            
            Put v_Put = null;
            if ( v_Puts.containsKey(v_HData.getRowKey()) )
            {
                v_Put = v_Puts.get(v_HData.getRowKey());
                v_Put.add(Bytes.toBytes(v_HData.getFamilyName())
                         ,Bytes.toBytes(v_HData.getColumnName())
                         ,Bytes.toBytes(v_HData.getValue().toString()));
            }
            else
            {
                v_Put = new Put(Bytes.toBytes(v_HData.getRowKey()));
                v_Put.add(Bytes.toBytes(v_HData.getFamilyName())
                         ,Bytes.toBytes(v_HData.getColumnName())
                         ,Bytes.toBytes(v_HData.getValue().toString()));
                
                v_Puts.put(v_HData.getRowKey() ,v_Put);
            }
        }
        
        
        try
        {
            v_Table = this.getTable(i_TableName);
            
            v_Table.put(new ArrayList<Put>(v_Puts.values()));
            v_Table.flushCommits();
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
	}
	
	
	
	/**
     * 按行主键RowKey，获取一行信息
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @return
     */
    public Map<String ,Object> getRow(String i_TableName ,String i_RowKey)
    {
        return this.getRow(i_TableName ,new HData(i_RowKey));
    }
	
	
	
	/**
	 * 按行主键RowKey，获取一行信息
	 * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
	 * @return
	 */
	public Map<String ,Object> getRow(String i_TableName ,HData i_HData)
	{
	    if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Call method[getRow] TableName is null.");
        }
	    
	    if ( i_HData == null )
        {
            throw new NullPointerException("Call method[getRow] HData is null.");
        }
	    
	    if ( Help.isNull(i_HData.getRowKey()) )
	    {
	        throw new NullPointerException("Call method[getRow] HData.getRowKey() is null.");
	    }
	    
	    return this.core_getRow(i_TableName ,i_HData.getRowKey());
	}
	
	
	
	/**
     * 按行主键RowKey，获取一行信息
     * 
     * 因此方法要被其它方法循环调用，固不在此方法中做过多的判断
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @return
     */
    private Map<String ,Object> core_getRow(String i_TableName ,String i_RowKey)
    {
        Map<String ,Object> v_Ret    = newColMap();
        Get                 v_Get    = new Get(Bytes.toBytes(i_RowKey));
        Result              v_Result = null;
        HTable              v_Table  = null;
        
        try
        {
            v_Table  = this.getTable(i_TableName);
            v_Result = v_Table.get(v_Get);
            
            if ( this.resultType == ResultType.ResultType_String )
            {
                for(Cell v_Cell : v_Result.rawCells())
                {
                    v_Ret.put(Bytes.toString(CellUtil.cloneQualifier(v_Cell)), Bytes.toString(CellUtil.cloneValue(v_Cell)));
                }
            }
            else if ( this.resultType == ResultType.ResultType_HData )
            {
                for(Cell v_Cell : v_Result.rawCells())
                {
                    HData v_HData = new HData(i_RowKey ,Bytes.toString(CellUtil.cloneQualifier(v_Cell)) ,Bytes.toString(CellUtil.cloneValue(v_Cell)));
                    
                    v_HData.setFamilyName(Bytes.toString(CellUtil.cloneFamily(v_Cell)));
                    v_HData.setTimestamp(v_Cell.getTimestamp());
                    
                    v_Ret.put(v_HData.getColumnName() ,v_HData);
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 按行主键RowKey、列族名、字段名，获取一行信息中某一列具体的值
     * 
     * @param i_TableName   表名
     * @param i_RowKey      行主键
     * @param i_FamilyName  列族名
     * @param i_ColumnName  字段名
     * @return
     */
    public Object getValue(String i_TableName ,String i_RowKey ,String i_FamilyName ,String i_ColumnName)
    {
        return this.core_getValue(i_TableName ,new HData(i_RowKey ,i_ColumnName).setFamilyName(i_FamilyName));
    }
    
    
    
    /**
     * 按行主键RowKey、列族名、字段名，获取一行信息中某一列具体的值
     * 
     * @param i_TableName    表名
     * @param i_HData        必有行主键、列族名、字段名
     * @return
     */
    public Object getValue(String i_TableName ,HData i_HData)
    {
        return this.core_getValue(i_TableName ,i_HData);
    }
    
    
    
    /**
     * 按行主键RowKey、列族名、字段名，获取一行信息中某一列具体的值
     * 
     * @param i_TableName    表名
     * @param i_HData        必有行主键、列族名、字段名
     * @return
     */
    private Object core_getValue(String i_TableName ,HData i_HData)
    {
        if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Call method[getValue] TableName is null.");
        }
        
        if ( i_HData == null )
        {
            throw new NullPointerException("Call method[getValue] HData is null.");
        }
        
        if ( Help.isNull(i_HData.getRowKey()) )
        {
            throw new NullPointerException("Call method[getValue] HData.getRowKey is null.");
        }
        
        if ( Help.isNull(i_HData.getFamilyName()) )
        {
            throw new NullPointerException("Call method[getValue] HData.getFamilyName is null.");
        }
        
        if ( Help.isNull(i_HData.getColumnName()) )
        {
            throw new NullPointerException("Call method[getValue] HData.getColumnName is null.");
        }
        
        
        Get                 v_Get    = new Get(Bytes.toBytes(i_HData.getRowKey()));
        FilterList          v_Filter = new FilterList();
        Result              v_Result = null;
        HTable              v_Table  = null;
        
        try
        {
            v_Filter.addFilter(new FamilyFilter(   i_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(i_HData.getFamilyName()))));
            v_Filter.addFilter(new QualifierFilter(i_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(i_HData.getColumnName()))));
            
            v_Get.setFilter(v_Filter);
            
            v_Table  = this.getTable(i_TableName);
            v_Result = v_Table.get(v_Get);
            
            if ( this.resultType == ResultType.ResultType_String )
            {
                for(Cell v_Cell : v_Result.rawCells())
                {
                    return Bytes.toString(CellUtil.cloneValue(v_Cell));
                }
            }
            else if ( this.resultType == ResultType.ResultType_HData )
            {
                for(Cell v_Cell : v_Result.rawCells())
                {
                    HData v_HData = new HData(i_HData.getRowKey() ,Bytes.toString(CellUtil.cloneQualifier(v_Cell)) ,Bytes.toString(CellUtil.cloneValue(v_Cell)));
                    
                    v_HData.setFamilyName(Bytes.toString(CellUtil.cloneFamily(v_Cell)));
                    v_HData.setTimestamp(v_Cell.getTimestamp());
                    
                    return v_HData;
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return null;
    }
    
    
    
    /**
     * 按行主键查询多行信息
     * 
     * @param i_TableName   表名称 
     * @param i_RowKeys     行主键集合
     * @return
     */
    public Map<String ,Map<String ,Object>> getRowsByKeys(String i_TableName ,List<String> i_RowKeys)
    {
        return this.getRowsByKeys(i_TableName ,i_RowKeys ,null);
    }
    
    
    
    /**
     * 按行主键查询多行信息
     * 
     * @param i_TableName    表名称 
     * @param i_RowKeys      行主键集合
     * @param i_ShowFilters  查询返回字段的过滤信息的集合
     * @return
     */
    public Map<String ,Map<String ,Object>> getRowsByKeys(String i_TableName ,List<String> i_RowKeys ,List<HData> i_ShowFilters)
    {
        if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Call method[getRowsByKey] TableName is null.");
        }
        
        if ( Help.isNull(i_RowKeys) )
        {
            throw new NullPointerException("Call method[getRowsByKey] RowKeys is null.");
        }
        
        
        for (int i=0; i<i_RowKeys.size(); i++)
        {
            if ( Help.isNull(i_RowKeys.get(i)) )
            {
                throw new NullPointerException("Call method[getRowsByKey] RowKeys.get(" + i + ") is null.");
            }
        }
        
        
        List<HData> v_HDatas = new ArrayList<HData>();
        
        if ( !Help.isNull(i_ShowFilters) )
        {
            for (HData v_HData : i_ShowFilters)
            {
                if ( v_HData.getValue() == null )
                {
                    if ( Help.isNull(v_HData.getRowKey()) )
                    {
                        if ( !Help.isNull(v_HData.getFamilyName()) )
                        {
                            v_HDatas.add(v_HData);
                        }
                    }
                }
            }
        }
        
        return this.core_getRowsByKeys(i_TableName ,i_RowKeys ,v_HDatas);
    }
    
	
    
	/**
     * 按条件获取相关行的信息，及行对应的所有字段信息
     * 
     * @param i_TableName   表名称 
     * @param i_Conditions  查询条件。i_Conditions.key 为字段名 ; i_Conditions.key 为字段值
     * @return
     */
	public Map<String ,Map<String ,Object>> getRows(String i_TableName ,Map<String ,Object> i_Conditions)
	{
        if ( !Help.isNull(i_Conditions) )
        {
            List<HData> v_HDatas = new ArrayList<HData>();
            
            for (String v_Key : i_Conditions.keySet())
            {
                v_HDatas.add(new HData(null ,v_Key ,i_Conditions.get(v_Key)));
            }
            
            return this.getRows(i_TableName ,v_HDatas.toArray(new HData []{}));
        }
        else
        {
            return this.getRows(i_TableName);
        }
	}
	
	
	
	/**
     * 按条件获取相关行的信息(只获取行主键)
     * 
     * @param i_TableName   表名称 
     * @param i_Conditions  查询条件。i_Conditions.key 为字段名 ; i_Conditions.key 为字段值
     * @return
     */
    public List<String> getRowsOnlyKey(String i_TableName ,Map<String ,Object> i_Conditions)
    {
        if ( !Help.isNull(i_Conditions) )
        {
            List<HData> v_HDatas = new ArrayList<HData>();
            
            for (String v_Key : i_Conditions.keySet())
            {
                v_HDatas.add(new HData(null ,v_Key ,i_Conditions.get(v_Key)));
            }
            
            return this.getRowsOnlyKey(i_TableName ,v_HDatas.toArray(new HData []{}));
        }
        else
        {
            return this.getRowsOnlyKey(i_TableName);
        }
    }
	
	
	/**
     * 按条件获取相关行的信息，及行对应的所有字段信息
     * 
     * 当 io_HPage.getRowKey() == null 时，就表示无数据了
     * 
     * @param i_TableName   表名称 
     * @param io_HPage      分页信息。输入输出型的参数
     * @param i_Conditions  查询条件。i_Conditions.key 为字段名 ; i_Conditions.key 为字段值
     * @return
     */
    public Map<String ,Map<String ,Object>> getRows(String i_TableName ,HPage io_HPage ,Map<String ,Object> i_Conditions)
    {
        if ( !Help.isNull(i_Conditions) )
        {
            List<HData> v_HDatas = new ArrayList<HData>();
            
            for (String v_Key : i_Conditions.keySet())
            {
                v_HDatas.add(new HData(null ,v_Key ,i_Conditions.get(v_Key)));
            }
            
            return this.core_getRows(i_TableName ,io_HPage ,v_HDatas.toArray(new HData []{}));
        }
        else
        {
            return this.getRows(i_TableName ,io_HPage);
        }
    }
	
	
	
	/**
     * 获取全表数据
     * 
     * @param i_TableName  表名称 
     * @return
     */ 
    public Map<String ,Map<String ,Object>> getRows(String i_TableName)
    {
        return this.core_getRows(i_TableName ,null ,null);
    }
    
    
    
    /**
     * 获取全表数据(只获取行主键)
     * 
     * @param i_TableName  表名称 
     * @return
     */ 
    public List<String> getRowsOnlyKey(String i_TableName)
    {
        return this.core_getRowsOnlyKey(i_TableName ,null ,null);
    }
    
    
    
    /**
     * 获取全表数据
     * 
     * 当 io_HPage.getRowKey() == null 时，就表示无数据了
     * 
     * @param i_TableName  表名称 
     * @param io_HPage     分页信息。输入输出型的参数
     * @return
     */ 
    public Map<String ,Map<String ,Object>> getRows(String i_TableName ,HPage io_HPage)
    {
        return this.core_getRows(i_TableName ,io_HPage ,null);
    }
	
	
	
	/**
     * 按条件获取相关行的信息，及行对应的所有字段信息
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表数据
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     * 
     * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
     * 
     * 6. 满足上面条件的同时，对查询返回结果的字段有过滤功能
     * 
     * @param i_TableName  表名称 
     * @param i_HDatas     查询条件
     * @return
     */ 
    public Map<String ,Map<String ,Object>> getRows(String i_TableName ,List<HData> i_HDatas)
    {
        if ( Help.isNull(i_HDatas) )
        {
            return this.core_getRows(i_TableName ,null ,null);
        }
        else
        {
            return this.core_getRows(i_TableName ,null ,i_HDatas.toArray(new HData []{}));
        }
    }
    
    
    
    /**
     * 按条件获取相关行的信息(只获取行主键)
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表数据
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     * 
     * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
     * 
     * 6. 满足上面条件的同时，对查询返回结果的字段有过滤功能
     * 
     * @param i_TableName  表名称 
     * @param i_HDatas     查询条件
     * @return
     */ 
    public List<String> getRowsOnlyKey(String i_TableName ,List<HData> i_HDatas)
    {
        if ( Help.isNull(i_HDatas) )
        {
            return this.core_getRowsOnlyKey(i_TableName ,null ,null);
        }
        else
        {
            return this.core_getRowsOnlyKey(i_TableName ,null ,i_HDatas.toArray(new HData []{}));
        }
    }
    
    
    
    /**
     * 按条件获取相关行的信息，及行对应的所有字段信息
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表数据
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     * 
     * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
     * 
     * 6. 满足上面条件的同时，对查询返回结果的字段有过滤功能
     * 
     * 
     * 当 io_HPage.getRowKey() == null 时，就表示无数据了
     * 
     * @param i_TableName  表名称 
     * @param io_HPage     分页信息。输入输出型的参数
     * @param i_HDatas     查询条件
     * @return
     */ 
    public Map<String ,Map<String ,Object>> getRows(String i_TableName ,HPage i_HPage ,List<HData> i_HDatas)
    {
        if ( Help.isNull(i_HDatas) )
        {
            return this.core_getRows(i_TableName ,i_HPage ,null);
        }
        else
        {
            return this.core_getRows(i_TableName ,i_HPage ,i_HDatas.toArray(new HData []{}));
        }
    }
    
    
    
	/**
	 * 按条件获取相关行的信息，及行对应的所有字段信息
	 * 
	 * 1. 当 i_HDatas 为空时，表示获取全表数据
	 * 
	 * 2. 当 HData.getRowKey 有值时，按RowKey匹配
	 * 
	 * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
	 * 
	 * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
	 *    按具体列族上的某个字段值匹配查询
	 * 
	 * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
	 * 
	 * 6. 满足上面条件的同时，对查询返回结果的字段有过滤功能
	 * 
	 * @param i_TableName  表名称 
	 * @param i_HDatas     查询条件
	 * @return
	 */ 
	public Map<String ,Map<String ,Object>> getRows(String i_TableName ,HData ... i_HDatas)
	{
	    return this.core_getRows(i_TableName ,null ,i_HDatas);
	}
	
	
	
	/**
     * 按条件获取相关行的信息(只获取行主键)
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表数据
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     * 
     * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
     * 
     * 6. 满足上面条件的同时，对查询返回结果的字段有过滤功能
     * 
     * 
     * @param i_TableName  表名称 
     * @param io_HPage     分页信息。输入输出型的参数
     * @param i_HDatas     查询条件
     * @return
     */ 
    public List<String> getRowsOnlyKey(String i_TableName ,HData ... i_HDatas)
    {
        return this.core_getRowsOnlyKey(i_TableName ,null ,i_HDatas);
    }
	
	
	
	/**
     * 按条件获取相关行的信息，及行对应的所有字段信息
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表数据
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     * 
     * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
     * 
     * 6. 满足上面条件的同时，对查询返回结果的字段有过滤功能
     * 
     * 
     * 当 io_HPage.getRowKey() == null 时，就表示无数据了
     * 
     * @param i_TableName  表名称 
     * @param io_HPage     分页信息。输入输出型的参数
     * @param i_HDatas     查询条件
     * @return
     */ 
    public Map<String ,Map<String ,Object>> getRows(String i_TableName ,HPage i_HPage ,HData ... i_HDatas)
    {
        return this.core_getRows(i_TableName ,i_HPage ,i_HDatas);
    }
	
	
	
	/**
     * 按条件获取相关行的信息，及行对应的所有字段信息
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表数据
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     * 
     * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
     * 
     * 6. 满足上面条件的同时，对查询返回结果的字段有过滤功能
     *    
     *    
     * 优化：只有按RowKey查询的情况，只向数据库发起一次访问操作
     * 
     * 如果启用了分页功能，当 io_HPage.getRowKey() == null 时，就表示无数据了
     * 
     * @param i_TableName  表名称 
     * @param io_HPage     分页信息。输入输出型的参数
     * @param i_HDatas     查询条件
     * @return
     */ 
    @SuppressWarnings("unused")
    private Map<String ,Map<String ,Object>> core_getRows(String i_TableName ,HPage io_HPage ,HData [] i_HDatas)
    {
        if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Call method[getRows] TableName is null.");
        }
        
        
        // 按查询顺序返回集合
        Map<String, Map<String, Object>> v_Ret         = newRowMap();
        Scan                             v_Scan        = new Scan();
        FilterList                       v_PFilters    = null;
        FilterList                       v_Filters     = null;
        List<String>                     v_RowKeys     = new ArrayList<String>();
        List<HData>                      v_ShowFilters = new ArrayList<HData>();
        ResultScanner                    v_Results     = null;
        byte []                          v_RowKey      = null;
        Return<?>                        v_RPFilter    = null;
        

        v_RPFilter = this.core_ParseFilter(i_HDatas ,v_RowKeys ,v_ShowFilters);
        v_Filters  = (FilterList)v_RPFilter.paramObj;
        

        if ( !v_RPFilter.booleanValue() )
        {
            if ( v_RPFilter.paramInt == 1 )
            {
                // 使用FirstKeyOnlyFilter会带来性能上的提升
                // v_PFilters = new FilterList();
                // v_PFilters.addFilter(new FirstKeyOnlyFilter());
            }
        }
        
        if ( io_HPage != null )
        {
            if ( v_PFilters == null )
            {
                v_PFilters = new FilterList();
            }
            
            if ( v_Filters.getFilters().size() == 1 )
            {
                v_PFilters.addFilter(v_Filters.getFilters().get(0));
            }
            else if ( v_Filters.getFilters().size() > 0 )
            {
                v_PFilters.addFilter(v_Filters);
            }
            
            v_PFilters.addFilter(new PageFilter(io_HPage.getPageSize()));
            
            v_Scan.setFilter(v_PFilters);
            
            if ( io_HPage != null && io_HPage.getRowKey() != null )
            {
                v_Scan.setStartRow(io_HPage.getStartRowKey());
            }
        }
        else if ( v_Filters.getFilters().size() == 1 )
        {
            if ( v_PFilters == null )
            {
                v_Scan.setFilter(v_Filters.getFilters().get(0));
            }
            else
            {
                v_PFilters.addFilter(v_Filters.getFilters().get(0));
                v_Scan.setFilter(v_PFilters);
            }
        }
        else if ( v_Filters.getFilters().size() > 0 )
        {
            if ( v_PFilters == null )
            {
                v_Scan.setFilter(v_Filters);
            }
            else
            {
                v_Scan.setFilter(v_PFilters);
            }
        }
        
        
        try
        {
            int v_ForCount = 0;
            
            if ( v_RPFilter.booleanValue() )
            {
                if ( !Help.isNull(v_RowKeys) )
                {
                    v_Ret = this.core_getRowsByKeys(i_TableName ,v_RowKeys ,v_ShowFilters);
                    
                    for (String v_RowKeyStr : v_Ret.keySet())
                    {
                        v_RowKey = Bytes.toBytes(v_RowKeyStr);
                        v_ForCount++;
                    }
                }
                // 类似于全表查询
                else
                {
                    v_Results  = this.getTable(i_TableName).getScanner(v_Scan);
                    v_RowKey   = core_ResultScanner(v_Results ,v_Ret);
                    v_ForCount = v_Ret.size();
                }
            }
            else
            {
                // Super分页
                if ( v_RPFilter.paramInt >= 2 )
                {
                    v_RowKeys  = new ArrayList<String>();
                    v_ForCount = 0;
                    
                    if ( io_HPage != null )
                    {
                        while ( true )
                        {
                            if ( io_HPage.getRowKey() != null )
                            {
                                v_Scan.setStartRow(io_HPage.getStartRowKey());
                            }
                            
                            v_Results = this.getTable(i_TableName).getScanner(v_Scan);
                            Iterator<Result> v_Iter      = v_Results.iterator();
                            int              v_IterCount = 0;
                            
                            while ( v_Iter.hasNext() && v_ForCount < io_HPage.getPageSize() )
                            {
                                Result v_Result = v_Iter.next();
                                
                                v_RowKey = v_Result.getRow();
                                v_IterCount++;
                                
                                if ( v_RPFilter.paramInt <= v_Result.size() )
                                {
                                    v_RowKeys.add(Bytes.toString(v_RowKey));
                                    v_ForCount++;
                                }
                            }
                            
                            v_Results.close();
                            
                            if ( v_ForCount == io_HPage.getPageSize() )
                            {
                                io_HPage.setRowKey(v_RowKey);
                                break;
                            }
                            else if ( v_IterCount == 0 )
                            {
                                io_HPage.setRowKey(new byte[]{});
                                break;
                            }
                            else
                            {
                                io_HPage.setRowKey(v_RowKey);
                            }
                        }
                    }
                    else
                    {
                        v_Results = this.getTable(i_TableName).getScanner(v_Scan);
                        
                        for (Result v_Result : v_Results)
                        {
                            if ( v_RPFilter.paramInt <= v_Result.size() )
                            {
                                v_RowKey = v_Result.getRow();
                                v_RowKeys.add(Bytes.toString(v_RowKey));
                                v_ForCount++;
                            }
                        }
                    }
                    
                    v_Ret = this.core_getRowsByKeys(i_TableName ,v_RowKeys ,v_ShowFilters);
                }
                else if ( v_RPFilter.paramInt == 1 )
                {
                    v_RowKeys  = new ArrayList<String>();
                    v_Results  = this.getTable(i_TableName).getScanner(v_Scan);
                    v_RowKey   = core_ResultScanner(v_Results ,v_RowKeys);
                    v_ForCount = v_RowKeys.size();
                    
                    v_Ret = this.core_getRowsByKeys(i_TableName ,v_RowKeys ,v_ShowFilters);
                }
                else
                {
                    // 对查询结果显示字段的过滤。
                    if ( !Help.isNull(v_ShowFilters) )
                    {
                        FilterList v_TempFilters = (FilterList)v_Scan.getFilter();
                        
                        if ( v_TempFilters == null )
                        {
                            v_TempFilters = new FilterList();
                        }
                        
                        for (int i=0; i<v_ShowFilters.size(); i++)
                        {
                            v_TempFilters.addFilter((Filter)v_ShowFilters.get(i).getValue());
                        }
                        
                        v_Scan.setFilter(v_TempFilters);
                    }
                    
                    v_Results  = this.getTable(i_TableName).getScanner(v_Scan);
                    v_RowKey   = core_ResultScanner(v_Results ,v_Ret);
                    v_ForCount = v_Ret.size();
                }
            }
            
            
            if ( io_HPage != null )
            {
                if ( v_ForCount == io_HPage.getPageSize() )
                {
                    io_HPage.setRowKey(v_RowKey);
                }
                else
                {
                    io_HPage.setRowKey(new byte[]{});
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        finally
        {
            if ( v_Results != null )
            {
                v_Results.close();
            }
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 将查询结果转为Map集合
     * 
     * @param i_Results
     * @param io_Datas
     * @return          返回最后一个位置上的RowKey值
     */
    private byte [] core_ResultScanner(ResultScanner i_Results ,Map<String ,Map<String ,Object>> io_Datas)
    {
        byte [] v_RowKey = null;
        
        if ( this.resultType == ResultType.ResultType_String )
        {
            for (Result v_Result : i_Results)
            {
                Map<String ,Object> v_RowInfo = newColMap();
                
                for(Cell v_Cell : v_Result.rawCells())
                {
                    v_RowInfo.put(Bytes.toString(CellUtil.cloneQualifier(v_Cell)), Bytes.toString(CellUtil.cloneValue(v_Cell)));
                }
                
                v_RowKey = v_Result.getRow();
                io_Datas.put(Bytes.toString(v_RowKey) ,v_RowInfo);
            }
        }
        else if ( this.resultType == ResultType.ResultType_HData )
        {
            for (Result v_Result : i_Results)
            {
                Map<String ,Object> v_RowInfo = newColMap();
                v_RowKey = v_Result.getRow();
                
                for(Cell v_Cell : v_Result.rawCells())
                {
                    HData v_HData = new HData(Bytes.toString(v_RowKey) ,Bytes.toString(CellUtil.cloneQualifier(v_Cell)) ,Bytes.toString(CellUtil.cloneValue(v_Cell)));
                    
                    v_HData.setFamilyName(Bytes.toString(CellUtil.cloneFamily(v_Cell)));
                    v_HData.setTimestamp(v_Cell.getTimestamp());
                    
                    v_RowInfo.put(v_HData.getColumnName(), v_HData);
                }
                
                io_Datas.put(Bytes.toString(v_RowKey) ,v_RowInfo);
            }
        }
        
        return v_RowKey;
    }
    
    
    
    /**
     * 按条件获取相关行的信息(只获取行主键)
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表数据
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     * 
     * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
     *    
     *    
     * 优化：只有按RowKey查询的情况，只向数据库发起一次访问操作
     * 
     * 如果启用了分页功能，当 io_HPage.getRowKey() == null 时，就表示无数据了
     * 
     * @param i_TableName  表名称 
     * @param io_HPage     分页信息。输入输出型的参数
     * @param i_HDatas     查询条件
     * @return
     */ 
    @SuppressWarnings("unused")
    private List<String> core_getRowsOnlyKey(String i_TableName ,HPage io_HPage ,HData [] i_HDatas)
    {
        if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Call method[getRows] TableName is null.");
        }
        
        
        // 按查询顺序返回集合
        List<String>  v_Ret         = null;
        Scan          v_Scan        = new Scan();
        FilterList    v_PFilters    = null;
        FilterList    v_Filters     = null;
        List<String>  v_RowKeys     = new ArrayList<String>();
        List<HData>   v_ShowFilters = new ArrayList<HData>();
        ResultScanner v_Results     = null;
        byte []       v_RowKey      = null;
        Return<?>     v_RPFilter    = null;
        

        v_RPFilter = this.core_ParseFilter(i_HDatas ,v_RowKeys ,v_ShowFilters);
        v_Filters  = (FilterList)v_RPFilter.paramObj;
        
        
        if ( !v_RPFilter.booleanValue() )
        {
            if ( v_RPFilter.paramInt == 1 )
            {
                // 使用FirstKeyOnlyFilter会带来性能上的提升
                // v_PFilters = new FilterList();
                // v_PFilters.addFilter(new FirstKeyOnlyFilter());
            }
        }
        
        
        if ( io_HPage != null )
        {
            if ( v_PFilters == null )
            {
                v_PFilters = new FilterList();
            }
            
            if ( v_Filters.getFilters().size() == 1 )
            {
                v_PFilters.addFilter(v_Filters.getFilters().get(0));
            }
            else if ( v_Filters.getFilters().size() > 0 )
            {
                v_PFilters.addFilter(v_Filters);
            }
            
            v_PFilters.addFilter(new PageFilter(io_HPage.getPageSize()));
            
            v_Scan.setFilter(v_PFilters);
            
            if ( io_HPage != null && io_HPage.getRowKey() != null )
            {
                v_Scan.setStartRow(io_HPage.getStartRowKey());
            }
        }
        else if ( v_Filters.getFilters().size() == 1 )
        {
            if ( v_PFilters == null )
            {
                v_Scan.setFilter(v_Filters.getFilters().get(0));
            }
            else
            {
                v_Scan.setFilter(v_PFilters);
            }
        }
        else if ( v_Filters.getFilters().size() > 0 )
        {
            if ( v_PFilters == null )
            {
                v_Scan.setFilter(v_Filters);
            }
            else
            {
                v_Scan.setFilter(v_PFilters);
            }
        }
        
        
        try
        {
            int v_ForCount = 0;
            
            if ( v_RPFilter.booleanValue()  )
            {
                if ( !Help.isNull(v_RowKeys) )
                {
                    v_Ret      = v_RowKeys;
                    v_ForCount = v_RowKeys.size();
                    v_RowKey   = Bytes.toBytes(v_RowKeys.get(v_RowKeys.size() - 1));
                }
                else
                {
                    v_RowKeys  = new ArrayList<String>();
                    v_Results  = this.getTable(i_TableName).getScanner(v_Scan);
                    v_RowKey   = core_ResultScanner(v_Results ,v_RowKeys);
                    v_ForCount = v_RowKeys.size();
                }
            }
            else
            {
                // Super分页
                if ( v_RPFilter.paramInt >= 2 )
                {
                    v_RowKeys  = new ArrayList<String>();
                    v_ForCount = 0;
                    
                    if ( io_HPage != null )
                    {
                        while ( true )
                        {
                            if ( io_HPage.getRowKey() != null )
                            {
                                v_Scan.setStartRow(io_HPage.getStartRowKey());
                            }
                            
                            v_Results = this.getTable(i_TableName).getScanner(v_Scan);
                            Iterator<Result> v_Iter      = v_Results.iterator();
                            int              v_IterCount = 0;
                            
                            while ( v_Iter.hasNext() && v_ForCount < io_HPage.getPageSize() )
                            {
                                Result v_Result = v_Iter.next();
                                
                                v_RowKey = v_Result.getRow();
                                v_IterCount++;
                                
                                if ( v_RPFilter.paramInt <= v_Result.size() )
                                {
                                    v_RowKeys.add(Bytes.toString(v_RowKey));
                                    v_ForCount++;
                                }
                            }
                            
                            v_Results.close();
                            
                            if ( v_ForCount == io_HPage.getPageSize() )
                            {
                                io_HPage.setRowKey(v_RowKey);
                                break;
                            }
                            else if ( v_IterCount == 0 )
                            {
                                io_HPage.setRowKey(new byte[]{});
                                break;
                            }
                            else
                            {
                                io_HPage.setRowKey(v_RowKey);
                            }
                        }
                    }
                    else
                    {
                        v_Results = this.getTable(i_TableName).getScanner(v_Scan);
                        
                        for (Result v_Result : v_Results)
                        {
                            if ( v_RPFilter.paramInt <= v_Result.size() )
                            {
                                v_RowKey = v_Result.getRow();
                                v_RowKeys.add(Bytes.toString(v_RowKey));
                                v_ForCount++;
                            }
                        }
                    }
                }
                else
                {
                    v_RowKeys  = new ArrayList<String>();
                    v_Results  = this.getTable(i_TableName).getScanner(v_Scan);
                    v_RowKey   = core_ResultScanner(v_Results ,v_RowKeys);
                    v_ForCount = v_RowKeys.size();
                }
                
                v_Ret = v_RowKeys;
            }
            
            
            if ( io_HPage != null )
            {
                if ( v_ForCount == io_HPage.getPageSize() )
                {
                    io_HPage.setRowKey(v_RowKey);
                }
                else
                {
                    io_HPage.setRowKey(new byte[]{});
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        finally
        {
            if ( v_Results != null )
            {
                v_Results.close();
            }
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 将查询结果转为List集合。并且只保存行主键
     * 
     * @param i_Results
     * @param io_Datas
     * @return          返回最后一个位置上的RowKey值
     */
    private byte [] core_ResultScanner(ResultScanner i_Results ,List<String> io_Datas)
    {
        byte [] v_RowKey = null;
        
        for (Result v_Result : i_Results)
        {
            v_RowKey = v_Result.getRow();
            io_Datas.add(Bytes.toString(v_RowKey));
        }
        
        return v_RowKey;
    }
    
    
    
    /**
     * 解释过滤器
     * 
     * @param i_HDatas       查询条件
     * @param o_RowKeys      输出行主键集合。输入为空时，表示不输出
     * @param o_ShowFilters  输出查询返回字段的过滤信息的集合。
     * @return               返回值含义：是否只有按RowKey查询的情况，将对这种情况优化
     *                       返回Return.paramInt表示：同行同时有多少个过滤条件
     *                       返回Return.paramObj表示：过滤器集合
     */
    private Return<FilterList> core_ParseFilter(HData [] i_HDatas ,List<String> o_RowKeys ,List<HData> o_ShowFilters)
    {
        Return<FilterList> v_Return  = new Return<FilterList>(true);
        FilterList         v_Filters = new FilterList(Operator.MUST_PASS_ONE);
        boolean            v_IsOR    = false;
        
        if ( i_HDatas != null )
        {
            for (int i=0; i<i_HDatas.length; i++)
            {
                HData  v_HData  = i_HDatas[i];
                Filter v_Filter = null;
                
                if ( v_HData.getOperator() == Operator.MUST_PASS_ONE )
                {
                    v_IsOR = true;
                }
                
                if ( !Help.isNull(v_HData.getRowKey()) )
                {
                    v_Filter = new RowFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getRowKey())));
                    
                    if ( o_RowKeys != null )
                    {
                        o_RowKeys.add(v_HData.getRowKey());
                    }
                }
                else
                {
                    v_Return.set(false);
                    
                    if ( Help.isNull(v_HData.getColumnName()) )
                    {
                        if ( !Help.isNull(v_HData.getFamilyName()) )
                        {
                            if ( v_HData.getValue() != null )
                            {
                                FilterList v_FilterList = new FilterList(Operator.MUST_PASS_ALL);
                                v_FilterList.addFilter(new FamilyFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getFamilyName()))));
                                
                                if ( v_HData.isLike() )
                                {
                                    v_FilterList.addFilter(new ValueFilter(v_HData.getCompareOp() ,new RegexStringComparator(v_HData.getValue().toString())));
                                }
                                else
                                {
                                    v_FilterList.addFilter(new ValueFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getValue().toString()))));
                                }
                                
                                v_Return.paramInt++;
                                v_Filter = v_FilterList;
                            }
                            else
                            {
                                v_Filter = new FamilyFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getFamilyName())));
                                o_ShowFilters.add(v_HData.clone().setValue(v_Filter)); // 待测试
                                v_Filter = null;
                            }
                        }
                        else
                        {
                            if ( v_HData.getValue() == null )
                            {
                                throw new NullPointerException("Call method[getRows] HDatas[" + i + "].getValue() is null.");
                            }
                            
                            if ( v_HData.isLike() )
                            {
                                v_Filter = new ValueFilter(v_HData.getCompareOp() ,new RegexStringComparator(v_HData.getValue().toString()));
                            }
                            else
                            {
                                v_Filter = new ValueFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getValue().toString())));
                            }
                            v_Return.paramInt++;
                        }
                    }
                    else if ( !Help.isNull(v_HData.getFamilyName()) )
                    {
                        if ( v_HData.getValue() != null )
                        {
                            FilterList v_FilterList = new FilterList(Operator.MUST_PASS_ALL);
                            
                            v_FilterList.addFilter(new FamilyFilter(   v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getFamilyName()))));
                            v_FilterList.addFilter(new QualifierFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getColumnName()))));
                            
                            if ( v_HData.isLike() )
                            {
                                v_FilterList.addFilter(new ValueFilter(v_HData.getCompareOp() ,new RegexStringComparator(v_HData.getValue().toString())));
                            }
                            else
                            {
                                v_FilterList.addFilter(new ValueFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getValue().toString()))));
                            }
                            
                            v_Filter = v_FilterList;
                            v_Return.paramInt++;
                        }
                        else
                        {
                            FilterList v_FilterList = new FilterList(Operator.MUST_PASS_ALL);
                            
                            v_FilterList.addFilter(new FamilyFilter(   v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getFamilyName()))));
                            v_FilterList.addFilter(new QualifierFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getColumnName()))));
                            
                            o_ShowFilters.add(v_HData.clone().setValue(v_FilterList));  // 待测试
                            v_Filter = null;
                        }
                    }
                    else if ( !Help.isNull(v_HData.getColumnName()) )
                    {
                        Filter v_QFilter = new QualifierFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getColumnName())));
                        
                        if ( v_HData.getValue() != null )
                        {
                            FilterList v_FilterList = new FilterList(Operator.MUST_PASS_ALL);
                            
                            v_FilterList.addFilter(v_QFilter);
                            
                            if ( v_HData.isLike() )
                            {
                                v_FilterList.addFilter(new ValueFilter(v_HData.getCompareOp() ,new RegexStringComparator(v_HData.getValue().toString())));
                            }
                            else
                            {
                                v_FilterList.addFilter(new ValueFilter(v_HData.getCompareOp() ,new BinaryComparator(Bytes.toBytes(v_HData.getValue().toString()))));
                            }
                            
                            v_Filter = v_FilterList;
                            v_Return.paramInt++;
                        }
                        else
                        {
                            // 对查询返回字段的过滤信息时，列族名必须存在才行
                            throw new VerifyError("Call method[getRows] HDatas[" + i +"] new ShowFilter is error, FamilyName must have.");
                        }
                    }
                    else
                    {
                        throw new NullPointerException("Call method[getRows] HDatas[" + i +"] new Filter is error.");
                    }
                }
                
                if ( v_Filter != null )
                {
                    v_Filters.addFilter(v_Filter);
                }
            }
        }
        
		
        if ( v_IsOR )
        {
            v_Return.paramInt = 1; 
        }
        
        return v_Return.paramObj(v_Filters);
    }
    
    
    
    /**
     * 按条件获取相关行的记录数
     * 
     * @param i_TableName   表名称 
     * @param i_Conditions  查询条件。i_Conditions.key 为字段名 ; i_Conditions.key 为字段值
     * @return
     */
    public long getCount(String i_TableName ,Map<String ,Object> i_Conditions)
    {
        if ( !Help.isNull(i_Conditions) )
        {
            List<HData> v_HDatas = new ArrayList<HData>();
            
            for (String v_Key : i_Conditions.keySet())
            {
                v_HDatas.add(new HData(null ,v_Key ,i_Conditions.get(v_Key)));
            }
            
            return this.getCount(i_TableName ,v_HDatas.toArray(new HData []{}));
        }
        else
        {
            return this.getCount(i_TableName);
        }
    }
    
    
    
    /**
     * 获取全表的记录数
     * 
     * @param i_TableName  表名称 
     * @return
     */
    public long getCount(String i_TableName)
    {
        return this.core_getCount(i_TableName ,new HPage(this.countPageSize) ,null);
    }
    
    
    
    /**
     * 按条件获取相关行的记录数
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表记录数
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     *    
     * 5. 当只有 HData.getFamilyName 也有值时，按列族名查询
     * 
     * @param i_TableName  表名称 
     * @param i_HDatas     查询条件
     * @return
     */
    public long getCount(String i_TableName ,List<HData> i_HDatas)
    {
        if ( Help.isNull(i_HDatas) )
        {
            return this.core_getCount(i_TableName ,new HPage(this.countPageSize) ,null);
        }
        else
        {
            return this.core_getCount(i_TableName ,new HPage(this.countPageSize) ,i_HDatas.toArray(new HData []{}));
        }
    }
    
    
    
    /**
     * 按条件获取相关行的记录数
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表记录数
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     * 
     * @param i_TableName  表名称 
     * @param i_HDatas     查询条件
     * @return
     */
    public long getCount(String i_TableName ,HData ... i_HDatas)
    {
        return this.core_getCount(i_TableName ,new HPage(this.countPageSize) ,i_HDatas);
    }
    
    
    
    /**
     * 按条件获取相关行的记录数
     * 
     * 1. 当 i_HDatas 为空时，表示获取全表记录数
     * 
     * 2. 当 HData.getRowKey 有值时，按RowKey匹配
     * 
     * 3. HData.getValue 有值，当 HData.getColumnName 为空时，按值匹配查询
     * 
     * 4. HData.getValue 有值，当 HData.getFamilyName、HData.getColumName 也有值时，
     *    按具体列族上的某个字段值匹配查询
     *    
     *    
     * 超级大表不在建议频繁调用此方法，原因是HBase数据库本身不适合做运算。
     * 如果是分页应用，建议使用如下方式：
     * 用户浏览数据首页时，选页栏显示 1 2 3 4 5 6.....，当用户点到第5页时，
     * 选页栏显示 ... 3 4 5 6 7 8 ....。
     * 不提供用户随意输页号，也不提供最后一页的按钮
     * 
     * @param i_TableName  表名称 
     * @param i_HPage      分页信息
     * @param i_HDatas     查询条件
     * @return
     */
    private long core_getCount(String i_TableName ,HPage i_HPage ,HData [] i_HDatas)
    {
        if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Call method[getCount] TableName is null.");
        }
        
        if ( i_HPage == null )
        {
            throw new NullPointerException("Call method[getCount] HPage is null.");
        }
        
        
        long               v_RCount      = 0; 
        Scan               v_Scan        = new Scan();
        FilterList         v_PFilter     = new FilterList();
        FilterList         v_Filters     = null;
        List<String>       v_RowKeys     = new ArrayList<String>();
        List<HData>        v_ShowFilters = new ArrayList<HData>();
        Return<FilterList> v_RPFilter    = null;
        ResultScanner      v_Results     = null;
        byte []            v_RowKey      = null;

        
        v_RPFilter = this.core_ParseFilter(i_HDatas ,null ,v_ShowFilters);
        v_Filters  = (FilterList)v_RPFilter.paramObj;
        
        
        if ( v_Filters.getFilters().size() >= 1 )
        {
            v_PFilter.addFilter(v_Filters); 
        }
        
        v_PFilter.addFilter(new PageFilter(i_HPage.getPageSize()));
        
        if ( !v_RPFilter.booleanValue() )
        {
            if ( v_RPFilter.paramInt == 1 )
            {
                // 使用FirstKeyOnlyFilter会带来性能上的提升
                // v_PFilter.addFilter(new FirstKeyOnlyFilter());   
            }
        }
        v_Scan.setFilter(v_PFilter);
        
        
        try
        {
            int v_ForCount = 0;
            
            if ( v_RPFilter.booleanValue() )
            {
                if ( !Help.isNull(v_RowKeys) )
                {
                    v_RCount = v_RowKeys.size();
                    v_RowKey = Bytes.toBytes(v_RowKeys.get(v_RowKeys.size() - 1));
                }
                else
                {
                    while ( true )
                    {
                        if ( i_HPage.getRowKey() != null )
                        {
                            v_Scan.setStartRow(i_HPage.getStartRowKey());
                        }
                        
                        v_Results  = this.getTable(i_TableName).getScanner(v_Scan);
                        v_ForCount = 0;
                        
                        for(Result v_Result : v_Results)
                        {
                            v_RowKey = v_Result.getRow();
                            v_RCount++;
                            v_ForCount++;
                        }
                        
                        v_Results.close();
                        
                        if ( v_ForCount == i_HPage.getPageSize() )
                        {
                            i_HPage.setRowKey(v_RowKey);
                        }
                        else
                        {
                            i_HPage.setRowKey(new byte[]{});
                            break;
                        }
                    }
                }
            }
            else
            {
                // Super分页 : Count
                if ( v_RPFilter.paramInt >= 2 )
                {
                    v_ForCount = 0;
                    
                    while ( true )
                    {
                        if ( i_HPage.getRowKey() != null )
                        {
                            v_Scan.setStartRow(i_HPage.getStartRowKey());
                        }
                        
                        v_Results = this.getTable(i_TableName).getScanner(v_Scan);
                        Iterator<Result> v_Iter      = v_Results.iterator();
                        int              v_IterCount = 0;
                        
                        while ( v_Iter.hasNext() && v_ForCount < i_HPage.getPageSize() )
                        {
                            Result v_Result = v_Iter.next();
                            
                            v_RowKey = v_Result.getRow();
                            v_IterCount++;
                            
                            if ( v_RPFilter.paramInt <= v_Result.size() )
                            {
                                v_RowKeys.add(Bytes.toString(v_RowKey));
                                v_ForCount++;
                                v_RCount++;
                            }
                        }
                        
                        v_Results.close();
                        
                        if ( v_IterCount == 0 )
                        {
                            i_HPage.setRowKey(new byte[]{});
                            break;
                        }
                        else
                        {
                            i_HPage.setRowKey(v_RowKey);
                        }
                    }
                }
                else
                {
                    // 对查询结果显示字段的过滤。
                    if ( !Help.isNull(v_ShowFilters) )
                    {
                        FilterList v_TempFilters = (FilterList)v_Scan.getFilter();
                        
                        if ( v_TempFilters == null )
                        {
                            v_TempFilters = new FilterList();
                        }
                        
                        for (int i=0; i<v_ShowFilters.size(); i++)
                        {
                            v_TempFilters.addFilter((Filter)v_ShowFilters.get(i).getValue());
                        }
                        
                        v_Scan.setFilter(v_TempFilters);
                    }
                    
                    while ( true )
                    {
                        if ( i_HPage.getRowKey() != null )
                        {
                            v_Scan.setStartRow(i_HPage.getStartRowKey());
                        }
                        
                        v_Results  = this.getTable(i_TableName).getScanner(v_Scan);
                        v_ForCount = 0;
                        
                        for(Result v_Result : v_Results)
                        {
                            v_RowKey = v_Result.getRow();
                            v_RCount++;
                            v_ForCount++;
                        }
                        
                        v_Results.close();
                        
                        if ( v_ForCount == i_HPage.getPageSize() )
                        {
                            i_HPage.setRowKey(v_RowKey);
                        }
                        else
                        {
                            i_HPage.setRowKey(new byte[]{});
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        finally
        {
            if ( v_Results != null )
            {
                v_Results.close();
            }
        }
        
        return v_RCount;
    }
    
    
    
    /**
     * 按行主键查询多行信息
     * 
     * 优化：只向数据库发起一次访问操作
     * 
     * 因此方法要被其它方法调用，固不在此方法中做过多的判断
     * 
     * @param i_TableName    表名称 
     * @param i_RowKeys      行主键集合
     * @param i_ShowFilters  查询返回字段的过滤信息的集合
     * @return
     */
    private Map<String ,Map<String ,Object>> core_getRowsByKeys(String i_TableName ,List<String> i_RowKeys ,List<HData> i_ShowFilters)
    {
        Map<String, Map<String, Object>> v_Ret     = newRowMap();
        List<Get>                        v_Gets    = new ArrayList<Get>();
        Result []                        v_Results = null;
        
        
        for (int i=0; i<i_RowKeys.size(); i++)
        {
            Get v_Get = new Get(Bytes.toBytes(i_RowKeys.get(i)));
            
            for (HData v_HData : i_ShowFilters)
            {
                if ( !Help.isNull(v_HData.getFamilyName()) )
                {
                    if ( !Help.isNull(v_HData.getColumnName()) )
                    {
                        v_Get.addColumn(Bytes.toBytes(v_HData.getFamilyName()) ,Bytes.toBytes(v_HData.getColumnName()));
                    }
                }
            }
            
            v_Gets.add(v_Get);
        }
        
        
        try
        {
            v_Results = this.getTable(i_TableName).get(v_Gets);
            
            if ( this.resultType == ResultType.ResultType_String )
            {
                for (Result v_Result : v_Results)
                {
                    if ( !v_Result.isEmpty() )
                    {
                        String              v_RowKey  = Bytes.toString(v_Result.getRow());
                        Map<String ,Object> v_RowInfo = newColMap();
                        
                        for(Cell v_Cell : v_Result.rawCells())
                        {
                            v_RowInfo.put(Bytes.toString(CellUtil.cloneQualifier(v_Cell)), Bytes.toString(CellUtil.cloneValue(v_Cell)));
                        }
                        
                        v_Ret.put(v_RowKey ,v_RowInfo);
                    }
                }
            }
            else if ( this.resultType == ResultType.ResultType_HData )
            {
                for (Result v_Result : v_Results)
                {
                    if ( !v_Result.isEmpty() )
                    {
                        String              v_RowKey  = Bytes.toString(v_Result.getRow());
                        Map<String ,Object> v_RowInfo = newColMap();
                        
                        for(Cell v_Cell : v_Result.rawCells())
                        {
                            HData v_HData = new HData(v_RowKey ,Bytes.toString(CellUtil.cloneQualifier(v_Cell)) ,Bytes.toString(CellUtil.cloneValue(v_Cell)));
                            
                            v_HData.setFamilyName(Bytes.toString(CellUtil.cloneFamily(v_Cell)));
                            v_HData.setTimestamp(v_Cell.getTimestamp());
                            
                            v_RowInfo.put(v_HData.getColumnName(), v_HData);
                        }
                        
                        v_Ret.put(v_RowKey ,v_RowInfo);
                    }
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return v_Ret;
    }
    
	
	
    /**
     * 删除一行记录
     * 
     * @param i_TableName  表名称 
     * @param i_RowKey     行主键
     */
    public void delete(String i_TableName ,String i_RowKey)
    {
        this.delete(i_TableName ,new HData(i_RowKey));
    }
    
    
    
    /**
     * 删除一行中某个列族下的所有字段信息
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @param i_FamilyName   列族
     */
    public void delete(String i_TableName ,String i_RowKey ,String i_FamilyName)
    {
        this.delete(i_TableName ,new HData(i_RowKey).setFamilyName(i_FamilyName));
    }
    
    
    
    /**
     * 删除一行中的某一字段
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @param i_FamilyName   列族
     * @param i_ColumnName   列名
     */
    public void delete(String i_TableName ,String i_RowKey ,String i_FamilyName ,String i_ColumnName)
    {
        this.delete(i_TableName ,new HData(i_RowKey ,i_ColumnName).setFamilyName(i_FamilyName));
    }
    
    
    
    /**
     * 删除一行中的同一列族的多个字段信息
     * 
     * @param i_TableName    表名
     * @param i_RowKey       行主键
     * @param i_FamilyName   列族
     * @param i_ColumnNames  列名集合
     */
    public void delete(String i_TableName ,String i_RowKey ,String i_FamilyName ,List<String> i_ColumnNames)
    {
        if ( Help.isNull(i_ColumnNames) )
        {
            throw new NullPointerException("Call method[delete] ColumnNames is null.");
        }
        
        
        List<HData> v_HDatas = new ArrayList<HData>();
        for (int i=0; i<i_ColumnNames.size(); i++)
        {
            String v_ColumnName = i_ColumnNames.get(i);
            
            if ( Help.isNull(v_ColumnName) )
            {
                throw new NullPointerException("Call method[delete] ColumnNames.get(" + i +") is null.");
            }
            
            v_HDatas.add(new HData(i_RowKey ,v_ColumnName).setFamilyName(i_FamilyName));
        }
        this.core_Delete(i_TableName ,v_HDatas.toArray(new HData []{}));
    }
    
    
    
    /**
     * 删除一行数据  或 删除一批数据
     * 
     * HData.RowKey 为必须存在的参数
     * 
     * 
     * 1. 当只有RowKey值时，删除一行数据，及相关字段
     * 
     * 2. 当存在 RowKey、ColumName、FamilyName 时，删除一行中的一列
     * 
     * 3. 当只存在 RowKey、FamilyName 时，删除一行中某个列族下的所有字段信息
     * 
     * @param i_TableName   表名称
     * @param i_HDatas      数据对象
     */
    public void delete(String i_TableName ,HData ... i_HDatas)
    {
        this.core_Delete(i_TableName ,i_HDatas);
    }
    
    
    
    /**
     * 删除一行数据  或 删除一批数据
     * 
     * HData.RowKey 为必须存在的参数
     *
     * 
     * 1. 当只有RowKey值时，删除一行数据，及相关字段
     * 
     * 2. 当存在 RowKey、ColumName、FamilyName 时，删除一行中的一列
     * 
     * 3. 当只存在 RowKey、FamilyName 时，删除一行中某个列族下的所有字段信息
     * 
     * @param i_TableName   表名称
     * @param i_HDatas      数据对象的集合
     */
    public void delete(String i_TableName ,List<HData> i_HDatas)
    {
        this.core_Delete(i_TableName ,i_HDatas.toArray(new HData []{}));
    }
    
    
    
    /**
     * 按行主键，删除一批数据
     * 
     * @param i_TableName   表名称
     * @param i_RowKeys     行主键的集合
     */
    public void deleteByKeys(String i_TableName ,List<String> i_RowKeys)
    {
        List<HData> v_HDatas = new ArrayList<HData>();
        
        for (int i=0; i<i_RowKeys.size(); i++)
        {
            if ( i_RowKeys.get(i) == null || Help.isNull(i_RowKeys.get(i).toString()) )
            {
                throw new NullPointerException("Call method[deleteByKeys] RowKeys.get(" + i + ") is null");
            }
            
            v_HDatas.add(new HData(i_RowKeys.get(i).toString()));
        }
        
        this.core_Delete(i_TableName ,v_HDatas.toArray(new HData []{}));
    }
    
    
    
    /**
     * 删除一行数据  或 删除一批数据
     * 
     * HData.RowKey 为必须存在的参数
     *
     * 
     * 1. 当只有RowKey值时，删除一行数据，及相关字段
     * 
     * 2. 当存在 RowKey、ColumName、FamilyName 时，删除一行中的一列
     * 
     * 3. 当只存在 RowKey、FamilyName 时，删除一行中某个列族下的所有字段信息
     * 
     * @param i_TableName   表名称
     * @param i_HDatas      数据对象的集合
     */
    private void core_Delete(String i_TableName ,HData [] i_HDatas)
    {
        if ( Help.isNull(i_TableName) )
        {
            throw new NullPointerException("Call method[delete] TableName is null.");
        }
        
        if ( i_HDatas == null || i_HDatas.length <= 0 )
        {
            throw new NullPointerException("Call method[delete] HDatas is null.");
        }
        
        
        
        HTable       v_Table   = null;
        List<Delete> v_Deletes = new ArrayList<Delete>();
        
        for (int i=0; i<i_HDatas.length; i++)
        {
            HData v_HData = i_HDatas[i];
            
            if ( Help.isNull(v_HData.getRowKey()) )
            {
                throw new NullPointerException("Call method[delete] HDatas[" + i + "].getRowKey() is null");
            }
            
            Delete v_Delete = new Delete(Bytes.toBytes(v_HData.getRowKey()));
            
            if ( !Help.isNull(v_HData.getColumnName()) )
            {
                if ( !Help.isNull(v_HData.getFamilyName()) )
                {
                    v_Delete.deleteColumn(Bytes.toBytes(v_HData.getFamilyName()) ,Bytes.toBytes(v_HData.getColumnName()));
                }
                else
                {
                    throw new NullPointerException("Call method[delete] HDatas[" + i +"].getFamilyName() is null");
                }
            }
            else if ( !Help.isNull(v_HData.getFamilyName()) )
            {
                v_Delete.deleteFamily(Bytes.toBytes(v_HData.getFamilyName()));
            }
            
            v_Deletes.add(v_Delete);
        }
        
        
        try
        {
            v_Table = this.getTable(i_TableName);
            
            v_Table.delete(v_Deletes);
            v_Table.flushCommits();
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
	
}
