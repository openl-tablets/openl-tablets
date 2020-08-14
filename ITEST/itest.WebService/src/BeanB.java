import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "BeanB", namespace = "xxx.yyy")
@XmlRootElement(namespace = "xxx.yyy")
public class BeanB {
    private String STR;
    private int In;

    @XmlElement(name = "STR")
    public String getSTR() {
        return STR;
    }

    public void setSTR(String STR) {
        this.STR = STR;
    }

    @XmlElement(name = "In")
    public int getIn() {
        return In;
    }

    public void setIn(int in) {
        In = in;
    }
}
