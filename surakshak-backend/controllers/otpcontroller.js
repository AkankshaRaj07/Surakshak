const Otp = require('../models/Otp');
const nodemailer = require('nodemailer');

// Set up Nodemailer transporter
const transporter = nodemailer.createTransport({
    service: 'gmail', // You can change this or use standard SMTP
    auth: {
        user: process.env.EMAIL_USER || 'your_email@gmail.com',
        pass: process.env.EMAIL_PASS || 'your_app_password'
    }
});

// Generate random 6-digit OTP
const generateOtp = () => {
    return Math.floor(100000 + Math.random() * 900000).toString();
};

// POST /api/otp/send
const sendOtp = async (req, res) => {
    const { mobile, email } = req.body;

    if ((!mobile || mobile.length !== 10) && !email) {
        return res.status(400).json({ message: 'Invalid mobile number or email' });
    }

    const otp = generateOtp();

    try {
        // Remove previous OTPs for this number/email
        const query = email ? { email } : { mobile };
        await Otp.deleteMany(query);

        // Save new OTP
        const newOtpData = { otp };
        if (email) newOtpData.email = email;
        if (mobile) newOtpData.mobile = mobile;
        const newOtp = new Otp(newOtpData);
        await newOtp.save().then(doc => {
            console.log("📦 OTP saved to MongoDB:", doc);
        }).catch(err => {
            console.error("❌ Failed to save OTP:", err);
        });

        console.log(`✅ OTP for ${email || mobile}: ${otp}`);  // Log OTP to console for judges

        // Send Email if email is provided
        if (email) {
            try {
                await transporter.sendMail({
                    from: `"Surakshak App" <${process.env.EMAIL_USER || 'noreply@surakshak.com'}>`,
                    to: email,
                    subject: "Your OTP for Surakshak",
                    text: `Your OTP is: ${otp}. It is valid for 5 minutes.`,
                    html: `<b>Your OTP is: ${otp}</b><br>It is valid for 5 minutes.`
                });
                console.log("📧 OTP sent via Email to:", email);
            } catch (mailErr) {
                console.error("❌ Failed to send email:", mailErr.message);
                console.log("Make sure EMAIL_USER and EMAIL_PASS are set in .env");
            }
        }

        // Send OTP in the response for frontend to display (or comment it out for production)
        res.json({
            message: 'OTP sent successfully',
            otp: otp  // Send the OTP here
        });
    } catch (error) {
        console.error('Error sending OTP:', error);
        res.status(500).json({ message: 'Server error while sending OTP' });
    }
};


// POST /api/otp/verify
const verifyOtp = async (req, res) => {
    const { mobile, email, otp } = req.body || {};  // Safely handle undefined req.body

    if ((!mobile && !email) || !otp) {
        return res.status(400).json({ message: 'Both identifier (mobile/email) and OTP are required' });
    }

    try {
        const query = email ? { email, otp } : { mobile, otp };
        const record = await Otp.findOne(query);

        if (!record) {
            return res.status(400).json({ message: 'Invalid or expired OTP' });
        }

        // OTP matched, delete it
        await Otp.deleteMany(email ? { email } : { mobile });

        res.json({ message: 'OTP verified successfully' });
    } catch (error) {
        console.error('Error verifying OTP:', error);
        res.status(500).json({ message: 'Server error while verifying OTP' });
    }
};

module.exports = { sendOtp, verifyOtp };
