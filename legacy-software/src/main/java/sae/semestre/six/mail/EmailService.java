package sae.semestre.six.mail;

public interface EmailService {

    public enum EMAIL_SOURCE {
        SUPPLIER,
        ADMIN,
        HOSPITAL;
        public String getEmail() {
            String email = "";
            switch (this) {
                case ADMIN:
                    email = "admin@hospital.com";
                    break;
                case SUPPLIER:
                    email = "supplier@example.com";
                    break;
                case HOSPITAL:
                    email = "hospital.system@gmail.com";
                    break;
                default:
                    throw new RuntimeException("The email can't be provided.");
            }
            return email;
        }
    }

    public void sendEmail(String to, String subject, String body) ;
}
