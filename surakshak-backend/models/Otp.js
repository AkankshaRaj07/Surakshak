const mongoose = require('mongoose');

const otpSchema = new mongoose.Schema({
    mobile: {
        type: String,
        required: false,
    },
    email: {
        type: String,
        required: false,
    },
    otp: {
        type: String,
        required: true,
        minlength: 6,
        maxlength: 6,
    },
    createdAt: {
        type: Date,
        default: Date.now,
        expires: 300 // Expires after 5 minutes (TTL Index)
    }
});

module.exports = mongoose.model('Otp', otpSchema);
