import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(namespace = "xxx.yyy", name="Bean")
@XmlRootElement(namespace = "xxx.yyy")
public class Bean {
    private int I;
    private BeanB BeanBName;

    public int getI() {
        return I;
    }

    @XmlElement(name = "I", required = true, defaultValue = "17")
    public void setI(int I) {
        this.I = I;
    }

    public BeanB getBeanBName() {
        return BeanBName;
    }

    public void setBeanBName(BeanB beanBName) {
        BeanBName = beanBName;
    }
}
