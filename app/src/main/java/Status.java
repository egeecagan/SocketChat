import java.util.Objects;

public class Status {
    private String name;
    private String message;

    public Status() {}

    public Status(String name, String message) {
        this.name = name;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + name + "] " + message;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, message);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) 
            return true;             

        if (obj == null || getClass() != obj.getClass()) 
            return false; 

        Status other = (Status) obj;                  
        return name.equals(other.name) && message.equals(other.message);           
    }
}

