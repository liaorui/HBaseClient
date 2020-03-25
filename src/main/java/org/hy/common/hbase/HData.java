package org.hy.common.hbase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList.Operator;
import org.hy.common.Date;
import org.hy.common.Help;





/**
 * HBase数据库的数据信息
 * 
 * 它是关系型数据库的一列(一个字段)的信息
 * 
 * @author ZhengWei(HY)
 * @create 2014-05-21
 */
public class HData implements Cloneable ,Comparable<HData>
{
    
    /** 排序类型 */
    public enum OrderType
    {
        /** 
         * HBase的默认排序
         * 按行主键、列族名及字段名排序 
         */
        OrderType_HBaseDefault
        
        /** 按时间戳排序 */
       ,OrderType_Timestamp
       
       /** 按字段值排序 */
       ,OrderType_Value
    }
    
    
    
    /** 行主键 */
    private String    rowKey;
    
    /** 字段名(不包含列族名) */
    private String    columnName;
    
    /** 字段值 */
    private Object    value;

    /** 列族名 */
    private String    familyName;
    
    /** 时间戳 */
    private long      timestamp;
    
    /** 比较类型 */
    private CompareOp compareOp;
    
    /** 
     * 多个HData间的关系。
     * 默认为 AND 关系。
     * 
     * 多个HData间，只要有一个是 OR 关系时，多个HData间的关系就都为 OR 关系
     */
    private Operator  operator;
    
    /** 
     * 针对字段值，在查询时，是否启用Like模式。
     * 
     *  Like模式下，this.value可以为正则表达式
     */
    private boolean   isLike;
    
    /** 排序比较的类型 */
    private OrderType orderType;
    
    
    
    public HData()
    {
        
    }
    
    
    
    public HData(String i_RowKey)
    {
        this(i_RowKey ,null ,null);
    }
    
    
    
    public HData(String i_RowKey ,String i_ColumnName)
    {
        this(i_RowKey ,i_ColumnName ,null);
    }
    
    
    
    public HData(String i_RowKey ,String i_ColumnName ,Object i_Value)
    {
        this.rowKey     = i_RowKey;
        this.columnName = i_ColumnName;
        this.value      = i_Value;
    }
    
    
    
    public HData(CompareOp i_CompareOp ,String i_Value)
    {
        this(i_CompareOp ,null ,null ,i_Value);
    }
    
    
    
    public HData(CompareOp i_CompareOp ,String i_FamilyName ,String i_ColumnName ,String i_Value)
    {
        this.compareOp  = i_CompareOp;
        this.familyName = i_FamilyName;
        this.columnName = i_ColumnName;
        this.value      = i_Value;
    }
    
    
    
    public String getRowKey()
    {
        return rowKey;
    }


    
    public HData setRowKey(String rowKey)
    {
        this.rowKey = rowKey;
        
        return this;
    }

    
    
    public String getColumnName()
    {
        return columnName;
    }

    
    
    public HData setColumnName(String columnName)
    {
        this.columnName = columnName;
        
        return this;
    }

    
    
    public Object getValue()
    {
        return value;
    }

    
    
    public HData setValue(Object value)
    {
        this.value = value;
        
        return this;
    }


    
    public String getFamilyName()
    {
        return familyName;
    }


    
    public HData setFamilyName(String familyName)
    {
        this.familyName = familyName;
        
        return this;
    }
    
    
    
    public Date getTime()
    {
        return new Date(this.timestamp);
    }

    
    public long getTimestamp()
    {
        return timestamp;
    }

    
    
    public HData setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
        
        return this;
    }


    
    public CompareOp getCompareOp()
    {
        return this.compareOp == null ? CompareOp.EQUAL : this.compareOp;
    }

    
    
    public HData setCompareOp(CompareOp compareOp)
    {
        this.compareOp = compareOp;
        
        return this;
    }


    
    public Operator getOperator()
    {
        return this.operator == null ? Operator.MUST_PASS_ALL : this.operator;
    }


    
    public HData setOperator(Operator operator)
    {
        this.operator = operator;
        
        return this;
    }
    
	
    
    public HData setOperator_OR()
    {
        this.operator = Operator.MUST_PASS_ONE;
        
        return this;
    }
    
    
    
    public HData setOperator_AND()
    {
        this.operator = Operator.MUST_PASS_ALL;
        
        return this;
    }
    
    
    
    public boolean isLike()
    {
        return isLike;
    }


    
    public HData setLike(boolean i_IsLike)
    {
        this.isLike = i_IsLike;
        
        return this;
    }

    
    
    public OrderType getOrderType()
    {
        return this.orderType == null ? OrderType.OrderType_HBaseDefault : this.orderType;
    }

    
    
    public HData setOrderType(OrderType orderType)
    {
        this.orderType = orderType;
        
        return this;
    }

    

    @Override
    public HData clone()
    {
        HData v_Clone = new HData(this.getRowKey() ,this.getColumnName() ,this.getValue()).setFamilyName(this.getFamilyName());
        
        v_Clone.setTimestamp(this.getTimestamp());
        v_Clone.setCompareOp(this.getCompareOp());
        v_Clone.setOperator( this.getOperator());
        v_Clone.setLike(     this.isLike());
        v_Clone.setOrderType(this.orderType);
        
        return v_Clone;
    }
    
    
    
    @Override
    public int compareTo(HData i_Other)
    {
        if ( i_Other == null )
        {
            return 1;
        }
        
        if ( this.getOrderType() == OrderType.OrderType_Timestamp )
        {
           if ( this.getTimestamp() > i_Other.getTimestamp() )
           {
               return 1;
           }
           else if ( this.getTimestamp() < i_Other.getTimestamp() )
           {
               return -1;
           }
           else
           {
               return 0;
           }
        }
        else if ( this.getOrderType() == OrderType.OrderType_Value )
        {
            return this.compareTo(this.getValue() ,i_Other.getValue());
        }
        else if ( this.getOrderType() == OrderType.OrderType_HBaseDefault )
        {
            int v_Ret = this.compareTo(this.getRowKey() ,i_Other.getRowKey());
            
            if ( v_Ret == 0 )
            {
                v_Ret = this.compareTo(this.getFamilyName() ,i_Other.getFamilyName());
                
                if ( v_Ret == 0 )
                {
                    return this.compareTo(this.getColumnName() ,i_Other.getColumnName());
                }
                else
                {
                    return v_Ret;
                }
            }
            else
            {
                return v_Ret;
            }
        }
        
        return 0;
    }
    
    
    
    private int compareTo(Object i_Me ,Object i_Other)
    {
        if ( i_Me == null )
        {
            if ( i_Other == null )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        else if ( i_Other == null )
        {
            return 1;
        }
        else
        {
            return i_Me.toString().compareTo(i_Other.toString());
        }
    }
    
    
    
    /**
     * 转为 put 命令
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-02-11
     * @version     v1.0
     *
     * @param i_TableName
     * @return
     */
    public String toPut(String i_TableName)
    {
        StringBuilder v_Buffer = new StringBuilder(); 
        
        v_Buffer.append("put '").append(Help.NVL(i_TableName)).append("'");
        
        // 行主键
        v_Buffer.append(",'");
        v_Buffer.append(Help.NVL(this.rowKey));
        v_Buffer.append("'");
        
        // 列族名:字段名
        v_Buffer.append(",'");
        v_Buffer.append(Help.NVL(this.familyName));
        v_Buffer.append(":");
        v_Buffer.append(Help.NVL(this.columnName));
        v_Buffer.append("'");
        
        // 字段值
        v_Buffer.append(",'");
        v_Buffer.append(Help.NVL(this.value));
        v_Buffer.append("';");
        
        return v_Buffer.toString();
    }
    
    
    
    @Override
    public String toString()
    {
        return this.value == null ? null : this.value.toString();
    }
    
    
    
    /**
     * 将Map转为List<HData>集合。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-03-18
     * @version     v1.0
     *
     * @param i_RowKey        行主键
     * @param i_FamilyName    列簇名称
     * @param i_ColumnValues  Map.key，即为字段名称，Map.value即为字段值
     * @return
     */
    public static List<HData> toHDatas(String i_RowKey ,String i_FamilyName ,Map<String ,Object> i_ColumnValues)
    {
        List<HData> v_Ret = null;
        
        if ( !Help.isNull(i_ColumnValues) )
        {
            v_Ret = new ArrayList<HData>(i_ColumnValues.size());
            
            for (Entry<String ,Object> v_Data : i_ColumnValues.entrySet())
            {
                if ( !Help.isNull(v_Data.getKey()) )
                {
                    String v_Value = v_Data.getValue() == null ? "" : v_Data.getValue().toString();
                    
                    v_Ret.add(new HData(i_RowKey ,v_Data.getKey().trim()  ,v_Value).setFamilyName(i_FamilyName));
                }
            }
        }
        else
        {
            v_Ret = new ArrayList<HData>();
        }
        
        return v_Ret;
    }
    
}
